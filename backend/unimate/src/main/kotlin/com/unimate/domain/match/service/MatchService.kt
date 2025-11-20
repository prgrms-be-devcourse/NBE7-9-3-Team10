package com.unimate.domain.match.service

import com.unimate.domain.chatroom.service.ChatroomService
import com.unimate.domain.match.dto.*
import com.unimate.domain.match.dto.MatchRecommendationResponse.MatchRecommendationItem
import com.unimate.domain.match.dto.MatchStatusResponse.SummaryInfo
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.Match.Companion.createLike
import com.unimate.domain.match.entity.Match.Companion.createRematch
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.message.service.MessageService
import com.unimate.domain.notification.entity.NotificationType
import com.unimate.domain.notification.service.NotificationService
import com.unimate.domain.review.service.MatchRematchService
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.entity.UserProfile.Companion.fromCached
import com.unimate.global.exception.ServiceException.Companion.badRequest
import com.unimate.global.exception.ServiceException.Companion.conflict
import com.unimate.global.exception.ServiceException.Companion.forbidden
import com.unimate.global.exception.ServiceException.Companion.internalServerError
import com.unimate.global.exception.ServiceException.Companion.notFound
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
@Transactional(readOnly = true)
class MatchService(
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val similarityCalculator: SimilarityCalculator,
    private val matchFilterService: MatchFilterService,
    private val matchUtilityService: MatchUtilityService,
    private val chatroomService: ChatroomService,
    private val notificationService: NotificationService,
    private val userMatchPreferenceRepository: UserMatchPreferenceRepository,
    private val matchCacheService: MatchCacheService,
    private val matchRematchService: MatchRematchService,
    private val messageService: MessageService
) {

    companion object {
        private val log = LoggerFactory.getLogger(MatchService::class.java)
    }

    // 룸메이트 추천 목록 조회 (Redis 캐시 사용)
    fun getMatchRecommendations(
        senderEmail: String,
        sleepPatternFilter: String?,
        ageRangeFilter: String?,
        cleaningFrequencyFilter: String?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): MatchRecommendationResponse {
        return getMatchRecommendationsWithCache(
            senderEmail,
            sleepPatternFilter,
            ageRangeFilter,
            cleaningFrequencyFilter,
            startDate,
            endDate
        )
    }

    // Redis 캐시 사용 버전
    private fun getMatchRecommendationsWithCache(
        senderEmail: String,
        sleepPatternFilter: String?,
        ageRangeFilter: String?,
        cleaningFrequencyFilter: String?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): MatchRecommendationResponse {
        val sender = getUserByEmail(senderEmail)
        val senderId = sender.id ?: throw internalServerError("송신자 ID가 null입니다.")

        val senderPreference = userMatchPreferenceRepository.findByUserId(senderId)
            .orElseThrow {
                notFound("사용자의 매칭 선호도를 찾을 수 없습니다. 먼저 선호도를 등록해주세요.")
            }

        val cachedCandidates = matchCacheService.getAllCandidates()
        log.info("Redis에서 {} 명의 후보 조회", cachedCandidates.size)

        val filteredCandidates = filterCachedCandidates(
            cachedCandidates,
            senderId,
            sender.gender,
            sender.university,
            sleepPatternFilter,
            ageRangeFilter,
            cleaningFrequencyFilter,
            startDate,
            endDate
        )

        val recommendations = buildCachedRecommendations(filteredCandidates, senderPreference, senderId)

        return MatchRecommendationResponse(recommendations)
    }

    /**
     * 이메일로 사용자 조회
     */
    private fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            ?: throw notFound("사용자를 찾을 수 없습니다.")
    }


    /**
     * CachedUserProfile을 UserProfile로 변환 (기존 유사도 계산 메서드 재사용용)
     */
    private fun convertToUserProfile(cached: CachedUserProfile): UserProfile {
        val user = User(
            cached.name,
            cached.email,
            "dummy_password",  // 유사도 계산에 필요 없음
            cached.gender,
            cached.birthDate,
            cached.university
        )
        if (cached.studentVerified) {
            user.studentVerified = true
        }

        return fromCached(user, cached)
    }

    // 캐시된 후보 필터링
    private fun filterCachedCandidates(
        allCandidates: List<CachedUserProfile>,
        senderId: Long,
        senderGender: Gender,
        senderUniversity: String,
        sleepPatternFilter: String?,
        ageRangeFilter: String?,
        cleaningFrequencyFilter: String?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<CachedUserProfile> {
        return allCandidates.asSequence()
            .filter { it.userId != senderId }
            .filter { it.gender == senderGender }
            .filter { it.matchingEnabled }
            .filter { userMatchPreferenceRepository.findByUserId(it.userId).isPresent }
            .filter { matchFilterService.applyUniversityFilter(it, senderUniversity) }
            .filter { !isAlreadyMatched(senderId, it.userId) }  // 이미 매칭된 사용자 제외
            .filter { matchFilterService.applySleepPatternFilter(it, sleepPatternFilter) }
            .filter { matchFilterService.applyAgeRangeFilter(it, ageRangeFilter) }
            .filter { matchFilterService.applyCleaningFrequencyFilter(it, cleaningFrequencyFilter) }
            .filter { matchFilterService.hasOverlappingPeriodByRange(it, startDate, endDate) }
            .toList()
    }

    // 캐시된 데이터로 추천 아이템 생성
    private fun buildCachedRecommendations(
        candidates: List<CachedUserProfile>,
        senderPreference: UserMatchPreference,
        senderId: Long
    ): List<MatchRecommendationItem> {
        return candidates
            .map { candidate ->
                buildCachedRecommendationItem(candidate, senderPreference, senderId)
            }
            .sortedByDescending { it.preferenceScore }
            .take(10)
    }

    // 캐시된 데이터로 개별 추천 아이템 생성
    private fun buildCachedRecommendationItem(
        candidate: CachedUserProfile,
        senderPreference: UserMatchPreference,
        senderId: Long
    ): MatchRecommendationItem {
        val candidateProfile = convertToUserProfile(candidate)
        
        // 실제 매칭 상태 조회 - 가장 최근 매칭 선택
        val allMatches = matchRepository.findAllBySenderIdAndReceiverId(senderId, candidate.userId)
        val existingMatch = allMatches.firstOrNull()
        
        // 유사도 점수 계산
        val similarityScore = BigDecimal.valueOf(
            similarityCalculator.calculateSimilarity(senderPreference, candidateProfile)
        )

        val matchType = existingMatch?.matchType ?: MatchType.NONE
        val matchStatus = existingMatch?.matchStatus ?: MatchStatus.NONE

        // 디버깅 로그
        log.info(
            "매칭 상태 조회 - senderId: {}, receiverId: {}, matchType: {}, matchStatus: {}",
            senderId, candidate.userId, matchType, matchStatus
        )

        return MatchRecommendationItem(
            candidate.userId,
            candidate.name,
            candidate.university,
            candidate.studentVerified,
            candidate.gender,
            matchUtilityService.calculateAge(candidate.birthDate),
            candidate.mbti,
            similarityScore,
            matchType,
            matchStatus,
            candidate.sleepTime,
            candidate.cleaningFrequency,
            candidate.isSmoker,
            candidate.startUseDate.toString(),
            candidate.endUseDate.toString()
        )
    }

    /**
     * 이미 매칭이 성사된 사용자인지 확인
     * REQUEST + ACCEPTED 상태인 경우 제외 (양쪽 모두 확정한 경우)
     * + REQUEST + PENDING 상태인 경우도 제외 (상호 좋아요로 채팅방이 열린 경우)
     */
    private fun isAlreadyMatched(senderId: Long, candidateId: Long): Boolean {
        // 양방향으로 모든 매칭 조회 후 필터링
        val allMatches = matchRepository.findAllMatchesBetweenUsers(senderId, candidateId)
        
        // REQUEST 타입이고 ACCEPTED 또는 PENDING 상태인 매칭이 있는지 확인
        return allMatches.any { match ->
            match.matchType == MatchType.REQUEST &&
            (match.matchStatus == MatchStatus.ACCEPTED || match.matchStatus == MatchStatus.PENDING)
        }
    }

    // 후보 프로필 상세 조회 (Redis 캐시 사용)
    fun getMatchRecommendationDetail(senderEmail: String, receiverId: Long): MatchRecommendationDetailResponse {
        return getMatchRecommendationDetailWithCache(senderEmail, receiverId)
    }

    // Redis 캐시 사용 버전
    private fun getMatchRecommendationDetailWithCache(
        senderEmail: String,
        receiverId: Long
    ): MatchRecommendationDetailResponse {
        val sender = userRepository.findByEmail(senderEmail)
            ?: throw notFound("사용자를 찾을 수 없습니다.")

        val senderId = sender.id ?: throw internalServerError("송신자 ID가 null입니다.")

        val senderPreference = userMatchPreferenceRepository.findByUserId(senderId)
            .orElseThrow { notFound("사용자의 매칭 선호도를 찾을 수 없습니다. 먼저 선호도를 등록해주세요.") }

        validateUserMatchPreference(receiverId)

        val cachedReceiver = matchCacheService.getUserProfileById(receiverId)
            ?: throw notFound("상대방 프로필을 찾을 수 없습니다.")

        val receiverProfile = convertToUserProfile(cachedReceiver)
        
        val similarityScore = BigDecimal.valueOf(
            similarityCalculator.calculateSimilarity(senderPreference, receiverProfile)
        )

        val existingMatch = matchRepository.findAllBySenderIdAndReceiverId(senderId, receiverId)
            .firstOrNull()

        val matchType = existingMatch?.matchType ?: MatchType.NONE
        val matchStatus = existingMatch?.matchStatus ?: MatchStatus.NONE

        return MatchRecommendationDetailResponse(
            cachedReceiver.userId,
            cachedReceiver.email,
            cachedReceiver.name,
            cachedReceiver.university,
            cachedReceiver.studentVerified,
            cachedReceiver.mbti,
            cachedReceiver.gender,
            matchUtilityService.calculateAge(cachedReceiver.birthDate),
            cachedReceiver.isSmoker,
            cachedReceiver.isPetAllowed,
            cachedReceiver.isSnoring,
            cachedReceiver.sleepTime,
            cachedReceiver.cleaningFrequency,
            cachedReceiver.hygieneLevel,
            cachedReceiver.noiseSensitivity,
            cachedReceiver.drinkingFrequency,
            cachedReceiver.guestFrequency,
            cachedReceiver.preferredAgeGap,
            cachedReceiver.birthDate,
            cachedReceiver.startUseDate,
            cachedReceiver.endUseDate,
            similarityScore,
            matchType,
            matchStatus
        )
    }

    /**
     * 룸메이트 최종 확정
     * 양방향 응답 추적: 각 사용자의 응답을 개별적으로 기록하고,
     * 양쪽 모두 확정해야만 최종 매칭 성사
     */
    @Transactional
    fun confirmMatch(matchId: Long, userId: Long): Match {
        val match = matchRepository.findByIdWithUsers(matchId)
            ?: throw notFound("매칭을 찾을 수 없습니다.")

        val senderId = match.sender.id ?: throw internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw internalServerError("수신자 ID가 null입니다.")

        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(senderId)
        validateUserMatchPreference(receiverId)

        validateMatchParticipant(match, userId)
        validateAndHandleMatchTypeTransition(match)

        // 이미 응답했는지 확인 (중복 응답 방지)
        if (match.hasUserResponded(userId)) {
            throw conflict("이미 응답을 완료했습니다.")
        }

        // 사용자의 확정 응답 처리 (Match 엔티티 내부에서 최종 상태 자동 결정)
        match.processUserResponse(userId, MatchStatus.ACCEPTED)

        // TODO: 향후 후기 시스템과 연계된 재매칭 기능 구현 시 rematch_round 활용
        matchRepository.save(match)

        return match
    }

    /**
     * 룸메이트 최종 거절
     * 양방향 응답 추적: 한 명이라도 거절하면 매칭 실패 처리
     */
    @Transactional
    fun rejectMatch(matchId: Long, userId: Long): Match {
        val match = matchRepository.findByIdWithUsers(matchId)
            ?: throw notFound("매칭을 찾을 수 없습니다.")

        val senderId = match.sender.id ?: throw internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw internalServerError("수신자 ID가 null입니다.")

        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(senderId)
        validateUserMatchPreference(receiverId)

        validateMatchParticipant(match, userId)
        validateAndHandleMatchTypeTransition(match)

        // 이미 응답했는지 확인 (중복 응답 방지)
        if (match.hasUserResponded(userId)) {
            throw conflict("이미 응답을 완료했습니다.")
        }

        // 사용자의 거절 응답 처리 (Match 엔티티 내부에서 최종 상태 자동 결정)
        match.processUserResponse(userId, MatchStatus.REJECTED)

        // TODO: 향후 후기 시스템과 연계된 재매칭 기능 구현 시 rematch_round 활용
        matchRepository.save(match)

        return match
    }

    /**
     * 매칭 상태 조회
     */
    fun getMatchStatus(userId: Long): MatchStatusResponse {
        val matches = matchRepository.findBySenderIdOrReceiverWithUsers(userId)

        val matchItems = matches
            .map { match -> matchUtilityService.toMatchStatusItem(match, userId) }
            .toList()

        val total = matches.size
        val pending = matches.count { it.matchStatus == MatchStatus.PENDING }
        val accepted = matches.count { it.matchStatus == MatchStatus.ACCEPTED }
        val rejected = matches.count { it.matchStatus == MatchStatus.REJECTED }

        val summary = SummaryInfo(
            total,
            pending,
            accepted,
            rejected
        )

        return MatchStatusResponse(matchItems, summary)
    }

    /**
     * 매칭 성사 결과 조회
     */
    fun getMatchResults(userId: Long): MatchResultResponse {
        val results = matchRepository.findBySenderIdOrReceiverWithUsers(userId)
            .filter { it.matchStatus == MatchStatus.ACCEPTED }
            .map { match -> matchUtilityService.toMatchResultItem(match) }

        return MatchResultResponse(results)
    }


    /**
     * 좋아요 보내기
     */
    @Transactional
    fun sendLike(requestDto: LikeRequest, senderId: Long): LikeResponse {
        val receiverId = requestDto.receiverId

        if (senderId == receiverId) {
            throw badRequest("자기 자신에게 '좋아요'를 보낼 수 없습니다.")
        }

        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(receiverId)

        val sender = userRepository.findById(senderId)
            .orElseThrow { notFound("전송하는 사용자를 찾을 수 없습니다.") }
        val receiver = userRepository.findById(receiverId)
            .orElseThrow { notFound("상대방 사용자를 찾을 수 없습니다.") }

        // (다시 좋아요) 만약 '좋아요 취소' 알림이 있었다면 삭제
        notificationService.deleteNotificationBySender(receiverId, NotificationType.LIKE_CANCELED, senderId)

        // 양방향으로 기존 '좋아요' 기록이 있는지 확인
        val existingMatches = matchRepository.findAllLikesBetweenUsers(senderId, receiverId)
        val existingMatch = existingMatches.firstOrNull()

        if (existingMatch != null) {
            // 기존 기록이 있는 경우
            val existingMatchSenderId = existingMatch.sender.id
                ?: throw internalServerError("기존 매칭의 송신자 ID가 null입니다.")

            if (existingMatchSenderId == senderId) {
                throw conflict("이미 해당 사용자에게 '좋아요'를 보냈습니다.")
            }

            // 상호 '좋아요' 성립: 기존 Match의 타입을 REQUEST로 변경하고 sender/receiver를 교체
            // 요청의 주체는 상호 '좋아요'를 완성시킨 현재 사용자(sender)가 됨
            existingMatch.upgradeToRequest(sender, receiver)
            matchRepository.save(existingMatch)

            var chatroomId: Long? = null
            try {
                val chatroomResponse = chatroomService.createIfNotExists(senderId, receiverId)
                chatroomId = chatroomResponse.chatroomId
            } catch (e: Exception) {
                // 채팅방 생성 실패해도 매칭은 진행
            }

            // 수정된 부분: 상호 좋아요 성사 알림 (매칭 알림) - 양쪽 모두에게 알림 전송
            try {
                // 받은 쪽에게 알림
                notificationService.createChatNotification(
                    receiverId,
                    NotificationType.MATCH,
                    "${sender.name} 님과 매칭되었습니다!",
                    sender.name,
                    senderId,
                    chatroomId
                )

                // 보낸 쪽에게도 알림
                notificationService.createChatNotification(
                    senderId,
                    NotificationType.MATCH,
                    "${receiver.name} 님과 매칭되었습니다!",
                    receiver.name,
                    receiverId,
                    chatroomId
                )
            } catch (e: Exception) {
                // 알림 생성 실패해도 매칭은 진행
            }

            val existingMatchId = existingMatch.id
                ?: throw internalServerError("기존 매칭 ID가 null입니다.")

            return LikeResponse(existingMatchId, true)
        } else {
            val senderPreference = userMatchPreferenceRepository.findByUserId(senderId)
                .orElseThrow { notFound("사용자의 매칭 선호도를 찾을 수 없습니다.") }

            // Redis 캐시에서 프로필 조회
            val cachedReceiver = matchCacheService.getUserProfileById(receiverId)
                ?: throw notFound("상대방 프로필을 찾을 수 없습니다.")

            val receiverProfile = convertToUserProfile(cachedReceiver)
            val preferenceScore = BigDecimal.valueOf(
                similarityCalculator.calculateSimilarity(senderPreference, receiverProfile)
            )

            // 기존 기록이 없는 경우 (처음 '좋아요')
            val newLike = createLike(sender, receiver, preferenceScore)
            matchRepository.save(newLike)

            // (연타 방지) 이미 보낸 '좋아요' 알림이 없다면 새로 생성
            if (!notificationService.notificationExistsBySender(receiverId, NotificationType.LIKE, senderId)) {
                notificationService.createNotification(
                    receiverId,
                    NotificationType.LIKE,
                    "${sender.name} 님이 회원님을 좋아합니다.",
                    sender.name,
                    senderId
                )
            }

            val newLikeId = newLike.id
                ?: throw internalServerError("새로운 매칭 ID가 null입니다.")

            return LikeResponse(newLikeId, false) // 아직 상호 매칭(요청)은 아님
        }
    }

    /**
     * 좋아요 취소
     */
    @Transactional
    fun cancelLike(senderId: Long, receiverId: Long) {
        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(receiverId)

        val sender = userRepository.findById(senderId)
            .orElseThrow { notFound("전송하는 사용자를 찾을 수 없습니다.") }

        val likeMatches = matchRepository.findAllBySenderReceiverAndType(senderId, receiverId, MatchType.LIKE)
        val like = likeMatches.firstOrNull() 
            ?: throw notFound("취소할 '좋아요' 기록이 존재하지 않습니다.")

        // 기존 '좋아요' 알림 삭제
        notificationService.deleteNotificationBySender(receiverId, NotificationType.LIKE, senderId)

        // '좋아요 취소' 알림이 없다면 새로 생성
        if (!notificationService.notificationExistsBySender(receiverId, NotificationType.LIKE_CANCELED, senderId)) {
            notificationService.createNotification(
                receiverId,
                NotificationType.LIKE_CANCELED,
                "${sender.name} 님이 좋아요를 취소했습니다.",
                sender.name,
                senderId
            )
        }

        matchRepository.delete(like)
    }

    /**
     * 매칭 참여자 권한 검증
     */
    private fun validateMatchParticipant(match: Match, userId: Long) {
        val senderId = match.sender.id ?: throw internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw internalServerError("수신자 ID가 null입니다.")

        if (senderId != userId && receiverId != userId) {
            throw forbidden("룸메이트 확정 권한이 없습니다.")
        }
    }

    /**
     * 매칭 타입 전이 처리 및 검증
     */
    private fun validateAndHandleMatchTypeTransition(match: Match) {
        when (match.matchType) {
            MatchType.LIKE -> {
                // LIKE -> REQUEST 전이 처리
                match.upgradeToRequest(match.sender, match.receiver)
            }

            MatchType.REQUEST -> {
                // 그대로 진행
            }

            else -> {
                throw badRequest("요청 상태가 아닌 매칭은 처리할 수 없습니다.")
            }
        }
    }

    private fun validateUserMatchPreference(userId: Long) {
        userMatchPreferenceRepository.findByUserId(userId)
            .orElseThrow { notFound("매칭 선호도가 등록되지 않은 사용자입니다.") }
    }

    /**
     * 재매칭 요청
     */
    @Transactional
    fun requestRematch(originalMatchId: Long, requesterId: Long): Match {
        val originalMatch = matchRepository.findByIdWithUsers(originalMatchId)
            ?: throw notFound("원본 매칭을 찾을 수 없습니다.")

        // 재매칭 가능 여부 확인 (후기 기반)
        val nextRound = matchRematchService.validateAndGetNextRound(originalMatch, requireReview = true)

        val senderId = originalMatch.sender.id ?: throw internalServerError("송신자 ID가 null입니다.")
        val receiverId = originalMatch.receiver.id ?: throw internalServerError("수신자 ID가 null입니다.")

        // 요청자가 매칭 참여자인지 확인
        require(requesterId == senderId || requesterId == receiverId) {
            throw forbidden("매칭 참여자만 재매칭을 요청할 수 있습니다.")
        }

        // 재매칭 요청자와 상대방 결정
        val rematchSender = if (requesterId == senderId) originalMatch.sender else originalMatch.receiver
        val rematchReceiver = if (requesterId == senderId) originalMatch.receiver else originalMatch.sender

        val rematchSenderId = rematchSender.id ?: throw internalServerError("재매칭 요청자 ID가 null입니다.")
        val rematchReceiverId = rematchReceiver.id ?: throw internalServerError("재매칭 상대방 ID가 null입니다.")

        // 기존 매칭과 동일한 사용자 조합의 재매칭이 이미 존재하는지 확인
        val allMatches = matchRepository.findBySenderIdOrReceiverWithUsers(rematchSenderId)
        val existingRematch = allMatches
            .filter { 
                (it.sender.id == rematchSenderId && it.receiver.id == rematchReceiverId) ||
                (it.sender.id == rematchReceiverId && it.receiver.id == rematchSenderId)
            }
            .firstOrNull { it.rematchRound == nextRound }

        if (existingRematch != null) {
            throw conflict("이미 재매칭 요청이 진행 중입니다.")
        }

        val preferenceScore = originalMatch.preferenceScore

        // 재매칭 생성
        val rematch = createRematch(
            sender = rematchSender,
            receiver = rematchReceiver,
            preferenceScore = preferenceScore,
            rematchRound = nextRound
        )

        val savedRematch = matchRepository.save(rematch)

        // 채팅방 생성 (기존 채팅방이 있으면 재사용)
        var chatroomId: Long? = null
        try {
            val chatroomResponse = chatroomService.createIfNotExists(rematchSenderId, rematchReceiverId)
            chatroomId = chatroomResponse.chatroomId
        } catch (e: Exception) {
            log.warn("재매칭 채팅방 생성 실패: ${e.message}")
        }

        // 재매칭 요청 알림 전송
        try {
            val senderName = rematchSender.name ?: "알 수 없음"
            notificationService.createNotification(
                userId = rematchReceiverId,
                type = NotificationType.MATCH,
                message = "${senderName}님이 재매칭을 요청했습니다.",
                senderName = senderName,
                senderId = rematchSenderId
            )
            log.info("재매칭 알림 전송 성공 - receiverId: {}, senderId: {}", rematchReceiverId, rematchSenderId)
        } catch (e: Exception) {
            log.error("재매칭 알림 전송 실패 - receiverId: {}, senderId: {}, error: {}", 
                rematchReceiverId, rematchSenderId, e.message, e)
        }

        // 채팅방에 재매칭 요청 메시지 전송
        if (chatroomId != null) {
            try {
                val senderName = rematchSender.name ?: "알 수 없음"
                val messageContent = "🔄 ${senderName}님이 재매칭을 요청했습니다. 채팅방에서 수락/거절할 수 있습니다."
                val clientMessageId = "rematch-${savedRematch.id}-${UUID.randomUUID()}"
                
                messageService.sendText(
                    me = rematchSenderId,
                    chatroomId = chatroomId,
                    content = messageContent,
                    clientMessageId = clientMessageId
                )
                log.info("재매칭 채팅방 메시지 전송 성공 - chatroomId: {}, senderId: {}", chatroomId, rematchSenderId)
            } catch (e: Exception) {
                log.warn("재매칭 채팅방 메시지 전송 실패 - chatroomId: {}, senderId: {}, error: {}", 
                    chatroomId, rematchSenderId, e.message)
            }
        }

        log.info("재매칭 요청 생성 완료 - originalMatchId: {}, rematchId: {}, round: {}", 
            originalMatchId, savedRematch.id, nextRound)

        return savedRematch
    }
}