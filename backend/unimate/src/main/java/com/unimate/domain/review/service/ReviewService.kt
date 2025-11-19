package com.unimate.domain.review.service

import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.notification.entity.NotificationType
import com.unimate.domain.notification.service.NotificationService
import com.unimate.domain.review.dto.PendingReviewResponse
import com.unimate.domain.review.dto.ReviewCreateRequest
import com.unimate.domain.review.dto.ReviewResponse
import com.unimate.domain.review.entity.Review
import com.unimate.domain.review.repository.ReviewRepository
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(ReviewService::class.java)

    @Value("\${review.min-days-after-match:90}")
    private val minDaysAfterMatch: Long = 90

    // 후기 작성
    @Transactional
    fun createReview(reviewerId: Long, request: ReviewCreateRequest): ReviewResponse {
        val match = matchRepository.findByIdWithUsers(request.matchId)
            ?: throw ServiceException.notFound("매칭을 찾을 수 없습니다.")

        // 후기 작성 가능 여부 확인
        validateReviewCreation(match)

        // 작성자 확인
        val reviewer = userRepository.findById(reviewerId)
            .orElseThrow { ServiceException.notFound("작성자를 찾을 수 없습니다.") }

        // 매칭 참여자 확인
        val senderId = match.sender.id ?: throw ServiceException.internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw ServiceException.internalServerError("수신자 ID가 null입니다.")

        require(reviewerId == senderId || reviewerId == receiverId) {
            throw ServiceException.forbidden("매칭 참여자만 후기를 작성할 수 있습니다.")
        }

        // 대상자 확인
        val reviewee = if (reviewerId == senderId) match.receiver else match.sender
        val revieweeId = reviewee.id ?: throw ServiceException.internalServerError("대상자 ID가 null입니다.")

        // 중복 작성 방지
        reviewRepository.findByMatchAndReviewerId(match, reviewerId)?.let {
            throw ServiceException.conflict("이미 후기를 작성했습니다.")
        }

        // 평점 검증
        require(request.rating in 1..5) {
            throw ServiceException.badRequest("평점은 1~5 사이여야 합니다.")
        }

        val review = Review(
            match = match,
            reviewer = reviewer,
            reviewee = reviewee,
            rating = request.rating,
            content = request.content,
            recommend = request.recommend,
            canRematch = request.recommend // 추천 = 재매칭 가능
        )

        val savedReview = reviewRepository.save(review)
        log.info(
            "후기 작성 완료 - matchId: {}, reviewerId: {}, rating: {}", match.id, reviewerId, request.rating
        )

        // 상대방에게 알림 전송
        try {
            notificationService.createNotification(
                userId = revieweeId,
                type = NotificationType.REVIEW,
                message = "${reviewer.name}님이 후기를 작성했습니다.",
                senderName = reviewer.name,
                senderId = reviewerId
            )
        } catch (e: Exception) {
            log.warn("리뷰 알림 전송 실패: ${e.message}")
            // 알림 전송 실패해도 리뷰 작성은 성공으로 처리
        }

        return ReviewResponse.from(savedReview)
    }

    /**
     * 후기 조회
     */
    fun getReview(reviewId: Long): ReviewResponse {
        val review = reviewRepository.findByIdWithRelations(reviewId)
            ?: throw ServiceException.notFound("후기를 찾을 수 없습니다.")

        return ReviewResponse.from(review)
    }

    /**
     * 특정 매칭의 후기 목록 조회
     */
    fun getReviewsByMatch(matchId: Long): List<ReviewResponse> {
        val match = matchRepository.findByIdWithUsers(matchId)
            ?: throw ServiceException.notFound("매칭을 찾을 수 없습니다.")

        return reviewRepository.findByMatch(match)
            .map { ReviewResponse.from(it) }
    }

    /**
     * 대기 중인 리뷰 목록 조회
     */
    fun getPendingReviews(userId: Long): List<PendingReviewResponse> {
        val matches =
            matchRepository.findBySenderIdOrReceiverWithUsers(userId).filter { it.matchStatus == MatchStatus.ACCEPTED }

        return matches.mapNotNull { match ->
            // 이미 후기 작성했는지 확인
            if (reviewRepository.findByMatchAndReviewerId(match, userId) != null) {
                return@mapNotNull null
            }

            // 대상자 정보
            val reviewee = if (match.sender.id == userId) match.receiver else match.sender
            val revieweeId = reviewee.id ?: return@mapNotNull null

            // 매칭 종료일 계산
            val senderId = match.sender.id ?: return@mapNotNull null
            val receiverId = match.receiver.id ?: return@mapNotNull null

            val senderProfile = userProfileRepository.findByUserId(senderId).orElse(null)
            val receiverProfile = userProfileRepository.findByUserId(receiverId).orElse(null)

            val matchEndDate = when {
                senderProfile != null && receiverProfile != null ->
                    minOf(senderProfile.endUseDate, receiverProfile.endUseDate)

                senderProfile != null -> senderProfile.endUseDate
                receiverProfile != null -> receiverProfile.endUseDate
                else -> match.confirmedAt?.toLocalDate() ?: return@mapNotNull null
            }

            // 후기 작성 가능 여부 및 남은 기간
            val canCreate = canCreateReview(match)
            val remainingDays = getRemainingDaysUntilReview(match, matchEndDate)

            PendingReviewResponse(
                matchId = match.id ?: return@mapNotNull null,
                revieweeId = revieweeId,
                revieweeName = reviewee.name,
                revieweeUniversity = reviewee.university,
                matchEndDate = matchEndDate.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
                canCreateReview = canCreate,
                remainingDays = remainingDays
            )
        }
    }

    /**
     * 후기 수정
     */
    @Transactional
    fun updateReview(
        reviewId: Long,
        reviewerId: Long,
        rating: Int?,
        content: String?,
        recommend: Boolean?
    ): ReviewResponse {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { ServiceException.notFound("후기를 찾을 수 없습니다.") }

        require(review.reviewer.id == reviewerId) {
            throw ServiceException.forbidden("후기 작성자만 수정할 수 있습니다.")
        }

        // 수정
        rating?.let {
            require(it in 1..5) {
                throw ServiceException.badRequest("평점은 1~5 사이여야 합니다.")
            }
            review.rating = it
        }

        content?.let { review.content = it }
        recommend?.let {
            review.recommend = it
            review.canRematch = it // 추천 = 재매칭 가능
        }

        val savedReview = reviewRepository.save(review)
        return ReviewResponse.from(savedReview)
    }

    /**
     * 후기 삭제
     */
    @Transactional
    fun deleteReview(reviewId: Long, reviewerId: Long) {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { ServiceException.notFound("후기를 찾을 수 없습니다.") }

        require(review.reviewer.id == reviewerId) {
            throw ServiceException.forbidden("후기 작성자만 삭제할 수 있습니다.")
        }

        reviewRepository.delete(review)
        log.info("후기 삭제 완료 - reviewId: {}, matchId: {}", reviewId, review.match.id)
    }

    /**
     * 후기 작성 가능 여부 확인
     */
    private fun validateReviewCreation(match: Match) {
        require(match.matchStatus == MatchStatus.ACCEPTED) {
            throw ServiceException.badRequest("매칭이 성사된 경우에만 후기를 작성할 수 있습니다.")
        }

        val confirmedAt = match.confirmedAt ?: throw ServiceException.badRequest("매칭 확정일시가 없어 후기를 작성할 수 없습니다.")

        // 매칭 성사 후 최소 1학기(90일) 경과 확인
        val daysSinceMatch = ChronoUnit.DAYS.between(confirmedAt, LocalDateTime.now())
        require(daysSinceMatch >= minDaysAfterMatch) {
            val remainingDays = minDaysAfterMatch - daysSinceMatch
            throw ServiceException.badRequest(
                "최소 1학기(약 ${minDaysAfterMatch}일) 이상 함께 생활한 후 후기를 작성할 수 있습니다. (현재: ${daysSinceMatch}일 경과, 남은 기간: 약 ${remainingDays}일)"
            )
        }

        // 매칭 기간 종료 확인
        val senderId = match.sender.id ?: throw ServiceException.internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw ServiceException.internalServerError("수신자 ID가 null입니다.")

        val senderProfile = userProfileRepository.findByUserId(senderId)
            .orElseThrow { ServiceException.notFound("송신자 프로필을 찾을 수 없습니다.") }

        val receiverProfile = userProfileRepository.findByUserId(receiverId)
            .orElseThrow { ServiceException.notFound("수신자 프로필을 찾을 수 없습니다.") }

        val earliestEndDate = minOf(senderProfile.endUseDate, receiverProfile.endUseDate)

        val today = LocalDate.now()
        require(today.isAfter(earliestEndDate) || today.isEqual(earliestEndDate)) {
            throw ServiceException.badRequest(
                "매칭 기간이 종료된 후에만 후기를 작성할 수 있습니다. (매칭 종료일: $earliestEndDate, 현재: $today)"
            )
        }

        log.debug(
            "후기 작성 조건 확인 완료 - matchId: {}, daysSinceMatch: {}, earliestEndDate: {}",
            match.id,
            daysSinceMatch,
            earliestEndDate
        )
    }

    /**
     * 후기 작성 가능 여부 확인
     */
    fun canCreateReview(match: Match): Boolean {
        return try {
            validateReviewCreation(match)
            true
        } catch (e: ServiceException) {
            false
        }
    }

    /**
     * 후기 작성까지 남은 기간 조회 (일)
     */
    fun getRemainingDaysUntilReview(match: Match, matchEndDate: LocalDate): Long? {
        if (match.matchStatus != MatchStatus.ACCEPTED) return null

        val confirmedAt = match.confirmedAt ?: return null
        val daysSinceMatch = ChronoUnit.DAYS.between(confirmedAt, LocalDateTime.now())

        // 1학기 경과 여부 확인
        val daysUntilMinPeriod = if (daysSinceMatch < minDaysAfterMatch) {
            minDaysAfterMatch - daysSinceMatch
        } else {
            0L
        }

        // 매칭 기간 종료 여부 확인
        val today = LocalDate.now()
        val daysUntilMatchEnd = if (today.isBefore(matchEndDate)) {
            ChronoUnit.DAYS.between(today, matchEndDate)
        } else {
            0L
        }

        // 둘 중 더 큰 값 반환 (둘 다 만족해야 하므로)
        return maxOf(daysUntilMinPeriod, daysUntilMatchEnd).takeIf { it > 0 }
    }

    /**
     * 양방향 후기 작성 완료 여부 확인
     */
    fun hasBothReviews(match: Match): Boolean {
        val senderId = match.sender.id ?: return false
        val receiverId = match.receiver.id ?: return false

        val reviews = reviewRepository.findByMatch(match)
        return reviews.any { it.reviewer.id == senderId } && reviews.any { it.reviewer.id == receiverId }
    }

    /**
     * 재매칭 가능 여부 확인
     */
    fun canRematch(match: Match): Boolean {
        val senderId = match.sender.id ?: return false
        val receiverId = match.receiver.id ?: return false

        val senderReview = reviewRepository.findByMatchAndReviewerId(match, senderId)
        val receiverReview = reviewRepository.findByMatchAndReviewerId(match, receiverId)

        return senderReview?.canRematch == true && receiverReview?.canRematch == true
    }

    /**
     * Review 기반 재매칭 가능 여부 확인 (MatchRematchService에서 사용)
     */
    fun canRematchBasedOnReviews(match: Match): Boolean {
        return hasBothReviews(match) && canRematch(match)
    }
}

