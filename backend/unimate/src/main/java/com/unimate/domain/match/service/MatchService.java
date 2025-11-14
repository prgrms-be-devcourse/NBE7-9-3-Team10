package com.unimate.domain.match.service;

import com.unimate.domain.chatroom.service.ChatroomService;
import com.unimate.domain.match.dto.*;
import com.unimate.domain.match.entity.Match;
import com.unimate.domain.match.entity.MatchStatus;
import com.unimate.domain.match.entity.MatchType;
import com.unimate.domain.match.repository.MatchRepository;
import com.unimate.domain.notification.entity.NotificationType;
import com.unimate.domain.notification.service.NotificationService;
import com.unimate.domain.user.user.entity.Gender;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.repository.UserRepository;
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference;
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository;
import com.unimate.domain.userProfile.entity.UserProfile;
import com.unimate.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final SimilarityCalculator similarityCalculator;
    private final MatchFilterService matchFilterService;
    private final MatchUtilityService matchUtilityService;
    private final ChatroomService chatroomService;
    private final NotificationService notificationService;
    private final UserMatchPreferenceRepository userMatchPreferenceRepository;
    private final MatchCacheService matchCacheService;

    // 룸메이트 추천 목록 조회 (Redis 캐시 사용)
    public MatchRecommendationResponse getMatchRecommendations(
            String senderEmail,
            String sleepPatternFilter,
            String ageRangeFilter,
            String cleaningFrequencyFilter,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getMatchRecommendationsWithCache(
                senderEmail, sleepPatternFilter, ageRangeFilter,
                cleaningFrequencyFilter, startDate, endDate
        );
    }

    // Redis 캐시 사용 버전
    private MatchRecommendationResponse getMatchRecommendationsWithCache(
            String senderEmail,
            String sleepPatternFilter,
            String ageRangeFilter,
            String cleaningFrequencyFilter,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User sender = getUserByEmail(senderEmail);
        UserMatchPreference senderPreference = userMatchPreferenceRepository.findByUserId(sender.getId())
                .orElseThrow(() -> ServiceException.notFound("사용자의 매칭 선호도를 찾을 수 없습니다. 먼저 선호도를 등록해주세요."));

        List<CachedUserProfile> cachedCandidates = matchCacheService.getAllCandidates();
        log.info("Redis에서 {} 명의 후보 조회", cachedCandidates.size());

        List<CachedUserProfile> filteredCandidates = filterCachedCandidates(
                cachedCandidates, sender.getId(), sender.getGender(), sender.getUniversity(),
                sleepPatternFilter, ageRangeFilter, cleaningFrequencyFilter, startDate, endDate
        );

        List<MatchRecommendationResponse.MatchRecommendationItem> recommendations =
                buildCachedRecommendations(filteredCandidates, senderPreference);

        return new MatchRecommendationResponse(recommendations);
    }

    /**
     * 이메일로 사용자 조회
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ServiceException.notFound("사용자를 찾을 수 없습니다."));
    }


    /**
     * CachedUserProfile을 UserProfile로 변환 (기존 유사도 계산 메서드 재사용용)
     */
    private UserProfile convertToUserProfile(CachedUserProfile cached) {
        User user = new User(
                cached.getName(),
                cached.getEmail(),
                "dummy_password", // 유사도 계산에 필요 없음
                cached.getGender(),
                cached.getBirthDate(),
                cached.getUniversity()
        );
        if (cached.getStudentVerified()) {
            user.verifyStudent();
        }

        return UserProfile.Companion.fromCached(user, cached);
    }

    // 캐시된 후보 필터링
    private List<CachedUserProfile> filterCachedCandidates(
            List<CachedUserProfile> allCandidates, Long senderId, Gender senderGender, String senderUniversity,
            String sleepPatternFilter, String ageRangeFilter, String cleaningFrequencyFilter,
            LocalDate startDate, LocalDate endDate
    ) {
        return allCandidates.stream()
                .filter(p -> !(p.getUserId() == senderId))
                .filter(p -> p.getGender().equals(senderGender))
                .filter(p -> p.getMatchingEnabled())
                .filter(p -> userMatchPreferenceRepository.findByUserId(p.getUserId()).isPresent())
                .filter(p -> matchFilterService.applyUniversityFilter(p, senderUniversity))
                .filter(p -> !isAlreadyMatched(senderId, p.getUserId())) // 이미 매칭된 사용자 제외
                .filter(p -> matchFilterService.applySleepPatternFilter(p, sleepPatternFilter))
                .filter(p -> matchFilterService.applyAgeRangeFilter(p, ageRangeFilter))
                .filter(p -> matchFilterService.applyCleaningFrequencyFilter(p, cleaningFrequencyFilter))
                .filter(p -> matchFilterService.hasOverlappingPeriodByRange(p, startDate, endDate))
                .toList();
    }

    // 캐시된 데이터로 추천 아이템 생성
    private List<MatchRecommendationResponse.MatchRecommendationItem> buildCachedRecommendations(
            List<CachedUserProfile> candidates, UserMatchPreference senderPreference) {
        return candidates.stream()
                .map(candidate -> buildCachedRecommendationItem(candidate, senderPreference))
                .sorted(Comparator.comparing(MatchRecommendationResponse.MatchRecommendationItem::getPreferenceScore).reversed())
                .limit(10)
                .toList();
    }

    // 캐시된 데이터로 개별 추천 아이템 생성
    private MatchRecommendationResponse.MatchRecommendationItem buildCachedRecommendationItem(
            CachedUserProfile candidate, UserMatchPreference senderPreference) {
        UserProfile candidateProfile = convertToUserProfile(candidate);
        BigDecimal similarityScore = BigDecimal.valueOf(similarityCalculator.calculateSimilarity(senderPreference, candidateProfile));

        // 실제 매칭 상태 조회
        Optional<Match> existingMatch = matchRepository.findBySenderIdAndReceiverId(
                senderPreference.getUser().getId(), candidate.getUserId());

        MatchType matchType = existingMatch.map(Match::getMatchType).orElse(MatchType.NONE);
        MatchStatus matchStatus = existingMatch.map(Match::getMatchStatus).orElse(MatchStatus.NONE);

        // 디버깅 로그
        log.info("매칭 상태 조회 - senderId: {}, receiverId: {}, matchType: {}, matchStatus: {}",
                senderPreference.getUser().getId(), candidate.getUserId(), matchType, matchStatus);

        return new MatchRecommendationResponse.MatchRecommendationItem(
                candidate.getUserId(),
                candidate.getName(),
                candidate.getUniversity(),
                candidate.getStudentVerified(),
                candidate.getGender(),
                matchUtilityService.calculateAge(candidate.getBirthDate()),
                candidate.getMbti(),
                similarityScore,
                matchType,
                matchStatus,
                // 추가 프로필 정보
                candidate.getSleepTime(),
                candidate.getCleaningFrequency(),
                candidate.isSmoker(),
                candidate.getStartUseDate() != null ? candidate.getStartUseDate().toString() : null,
                candidate.getEndUseDate() != null ? candidate.getEndUseDate().toString() : null
        );
    }

    /**
     * 이미 매칭이 성사된 사용자인지 확인
     * REQUEST + ACCEPTED 상태인 경우 제외 (양쪽 모두 확정한 경우)
     * + REQUEST + PENDING 상태인 경우도 제외 (상호 좋아요로 채팅방이 열린 경우)
     */
    private boolean isAlreadyMatched(Long senderId, Long candidateId) {
        // matchStatus == ACCEPTED는 오직 양쪽 모두 확정한 경우만. PENDING도 확인하도록 수정.
        boolean iSentAccepted = matchRepository.findBySenderIdAndReceiverId(senderId, candidateId)
                .map(match -> match.getMatchType() == MatchType.REQUEST && (match.getMatchStatus() == MatchStatus.ACCEPTED || match.getMatchStatus() == MatchStatus.PENDING))
                .orElse(false);
        
        // 상대방이 나에게 보낸 매칭이 ACCEPTED 상태인지 확인. PENDING도 확인하도록 수정.
        boolean theySentAccepted = matchRepository.findBySenderIdAndReceiverId(candidateId, senderId)
                .map(match -> match.getMatchType() == MatchType.REQUEST && (match.getMatchStatus() == MatchStatus.ACCEPTED || match.getMatchStatus() == MatchStatus.PENDING))
                .orElse(false);
        
        return iSentAccepted || theySentAccepted;
    }

    private void validateUserMatchPreference(Long userId) {
        userMatchPreferenceRepository.findByUserId(userId)
            .orElseThrow(() -> ServiceException.notFound("매칭 선호도가 등록되지 않은 사용자입니다."));
    }

    // 후보 프로필 상세 조회 (Redis 캐시 사용)
    public MatchRecommendationDetailResponse getMatchRecommendationDetail(String senderEmail, Long receiverId) {
        return getMatchRecommendationDetailWithCache(senderEmail, receiverId);
    }

    // Redis 캐시 사용 버전
    private MatchRecommendationDetailResponse getMatchRecommendationDetailWithCache(String senderEmail, Long receiverId) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> ServiceException.notFound("사용자를 찾을 수 없습니다."));

        UserMatchPreference senderPreference = userMatchPreferenceRepository.findByUserId(sender.getId())
                .orElseThrow(() -> ServiceException.notFound("사용자의 매칭 선호도를 찾을 수 없습니다. 먼저 선호도를 등록해주세요."));

        validateUserMatchPreference(receiverId);

        CachedUserProfile cachedReceiver = matchCacheService.getUserProfileById(receiverId);
        if (cachedReceiver == null) {
            throw ServiceException.notFound("상대방 프로필을 찾을 수 없습니다.");
        }

        UserProfile receiverProfile = convertToUserProfile(cachedReceiver);
        BigDecimal similarityScore = BigDecimal.valueOf(
            similarityCalculator.calculateSimilarity(senderPreference, receiverProfile)
        );

        Optional<Match> existingMatch = matchRepository.findBySenderIdAndReceiverId(sender.getId(), receiverId);

        MatchType matchType = existingMatch.map(Match::getMatchType).orElse(MatchType.NONE);
        MatchStatus matchStatus = existingMatch.map(Match::getMatchStatus).orElse(MatchStatus.NONE);

        return new MatchRecommendationDetailResponse(
                cachedReceiver.getUserId(),
                cachedReceiver.getEmail(),
                cachedReceiver.getName(),
                cachedReceiver.getUniversity(),
                cachedReceiver.getStudentVerified(),
                cachedReceiver.getMbti(),
                cachedReceiver.getGender(),
                matchUtilityService.calculateAge(cachedReceiver.getBirthDate()),
                cachedReceiver.isSmoker(),
                cachedReceiver.isPetAllowed(),
                cachedReceiver.isSnoring(),
                cachedReceiver.getSleepTime(),
                cachedReceiver.getCleaningFrequency(),
                cachedReceiver.getHygieneLevel(),
                cachedReceiver.getNoiseSensitivity(),
                cachedReceiver.getDrinkingFrequency(),
                cachedReceiver.getGuestFrequency(),
                cachedReceiver.getPreferredAgeGap(),
                cachedReceiver.getBirthDate(),
                cachedReceiver.getStartUseDate(),
                cachedReceiver.getEndUseDate(),
                similarityScore,
                matchType,
                matchStatus
        );
    }

    /**
     * 룸메이트 최종 확정
     * 양방향 응답 추적: 각 사용자의 응답을 개별적으로 기록하고, 
     * 양쪽 모두 확정해야만 최종 매칭 성사
     */
    @Transactional
    public Match confirmMatch(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> ServiceException.notFound("매칭을 찾을 수 없습니다."));

        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(match.getSender().getId());
        validateUserMatchPreference(match.getReceiver().getId());

        validateMatchParticipant(match, userId);
        validateAndHandleMatchTypeTransition(match);

        // 이미 응답했는지 확인 (중복 응답 방지)
        if (match.hasUserResponded(userId)) {
            throw ServiceException.conflict("이미 응답을 완료했습니다.");
        }

        // 사용자의 확정 응답 처리 (Match 엔티티 내부에서 최종 상태 자동 결정)
        match.processUserResponse(userId, MatchStatus.ACCEPTED);

        // TODO: 향후 후기 시스템과 연계된 재매칭 기능 구현 시 rematch_round 활용

        matchRepository.save(match);

        return match;
    }

    /**
     * 룸메이트 최종 거절
     * 양방향 응답 추적: 한 명이라도 거절하면 매칭 실패 처리
     */
    @Transactional
    public Match rejectMatch(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> ServiceException.notFound("매칭을 찾을 수 없습니다."));

        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(match.getSender().getId());
        validateUserMatchPreference(match.getReceiver().getId());

        validateMatchParticipant(match, userId);
        validateAndHandleMatchTypeTransition(match);

        // 이미 응답했는지 확인 (중복 응답 방지)
        if (match.hasUserResponded(userId)) {
            throw ServiceException.conflict("이미 응답을 완료했습니다.");
        }

        // 사용자의 거절 응답 처리 (Match 엔티티 내부에서 최종 상태 자동 결정)
        match.processUserResponse(userId, MatchStatus.REJECTED);

        // TODO: 향후 후기 시스템과 연계된 재매칭 기능 구현 시 rematch_round 활용

        matchRepository.save(match);

        return match;
    }

    /**
     * 매칭 상태 조회
     */
    public MatchStatusResponse getMatchStatus(Long userId) {
        List<Match> matches = matchRepository.findBySenderIdOrReceiverId(userId);

        List<MatchStatusResponse.MatchStatusItem> matchItems = matches.stream()
                .map(match -> matchUtilityService.toMatchStatusItem(match, userId))
                .toList();

        int total = matches.size();
        int pending = (int) matches.stream().filter(match -> match.getMatchStatus() == MatchStatus.PENDING).count();
        int accepted = (int) matches.stream().filter(match -> match.getMatchStatus() == MatchStatus.ACCEPTED).count();
        int rejected = (int) matches.stream().filter(match -> match.getMatchStatus() == MatchStatus.REJECTED).count();

        MatchStatusResponse.SummaryInfo summary = new MatchStatusResponse.SummaryInfo(
                total,
                pending,
                accepted,
                rejected
        );

        return new MatchStatusResponse(matchItems, summary);
    }

    /**
     * 매칭 성사 결과 조회
     */
    public MatchResultResponse getMatchResults(Long userId) {
        List<MatchResultResponse.MatchResultItem> results = matchRepository.findBySenderIdOrReceiverId(userId)
                .stream()
                .filter(match -> match.getMatchStatus() == MatchStatus.ACCEPTED)
                .map(match -> matchUtilityService.toMatchResultItem(match, userId))
                .toList();

        return new MatchResultResponse(results);
    }


    /**
     * 좋아요 보내기
     */
    @Transactional
    public LikeResponse sendLike(LikeRequest requestDto, Long senderId) {
        Long receiverId = requestDto.getReceiverId();

        if (senderId.equals(receiverId)) {
            throw ServiceException.badRequest("자기 자신에게 '좋아요'를 보낼 수 없습니다.");
        }

        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(receiverId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> ServiceException.notFound("전송하는 사용자를 찾을 수 없습니다."));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> ServiceException.notFound("상대방 사용자를 찾을 수 없습니다."));

        // (다시 좋아요) 만약 '좋아요 취소' 알림이 있었다면 삭제
        notificationService.deleteNotificationBySender(receiverId, NotificationType.LIKE_CANCELED, senderId);

        // 양방향으로 기존 '좋아요' 기록이 있는지 확인
        Optional<Match> existingMatchOpt = matchRepository.findLikeBetweenUsers(senderId, receiverId);

        if (existingMatchOpt.isPresent()) {
            // 기존 기록이 있는 경우
            Match existingMatch = existingMatchOpt.get();

            // 이미 요청(REQUEST) 단계이거나, 내가 이미 보낸 '좋아요'인 경우 중복 처리
            if (existingMatch.getMatchType() == MatchType.REQUEST) {
                throw ServiceException.conflict("이미 룸메이트 요청이 진행 중입니다.");
            }
            if (existingMatch.getSender().getId().equals(senderId)) {
                throw ServiceException.conflict("이미 해당 사용자에게 '좋아요'를 보냈습니다.");
            }

            // 상호 '좋아요' 성립: 기존 Match의 타입을 REQUEST로 변경하고 sender/receiver를 교체
            // 요청의 주체는 상호 '좋아요'를 완성시킨 현재 사용자(sender)가 됨
            existingMatch.upgradeToRequest(sender, receiver);
            matchRepository.save(existingMatch);

            Long chatroomId = null;
            try {
                var chatroomResponse = chatroomService.createIfNotExists(senderId, receiverId);
                chatroomId = chatroomResponse.getChatroomId();
            } catch (Exception e) {
                // 채팅방 생성 실패해도 매칭은 진행
            }

            // 수정된 부분: 상호 좋아요 성사 알림 (매칭 알림) - 양쪽 모두에게 알림 전송
            try {
                // 받은 쪽에게 알림
                notificationService.createChatNotification(
                        receiverId,
                        NotificationType.MATCH,
                        sender.getName() + " 님과 매칭되었습니다!",
                        sender.getName(),
                        senderId,
                        chatroomId
                );

                // 보낸 쪽에게도 알림
                notificationService.createChatNotification(
                        senderId,
                        NotificationType.MATCH,
                        receiver.getName() + " 님과 매칭되었습니다!",
                        receiver.getName(),
                        receiverId,
                        chatroomId
                );
            } catch (Exception e) {
                // 알림 생성 실패해도 매칭은 진행
            }



            return new LikeResponse(existingMatch.getId(), true);

        } else {
            UserMatchPreference senderPreference = userMatchPreferenceRepository.findByUserId(senderId)
                    .orElseThrow(() -> ServiceException.notFound("사용자의 매칭 선호도를 찾을 수 없습니다."));
            
            // Redis 캐시에서 프로필 조회
            CachedUserProfile cachedReceiver = matchCacheService.getUserProfileById(receiverId);
            if (cachedReceiver == null) {
                throw ServiceException.notFound("상대방 프로필을 찾을 수 없습니다.");
            }
            UserProfile receiverProfile = convertToUserProfile(cachedReceiver);
            
            BigDecimal preferenceScore = BigDecimal.valueOf(similarityCalculator.calculateSimilarity(senderPreference, receiverProfile));
            
            // 기존 기록이 없는 경우 (처음 '좋아요')
            Match newLike = Match.createLike(sender, receiver, preferenceScore);
            matchRepository.save(newLike);

            // (연타 방지) 이미 보낸 '좋아요' 알림이 없다면 새로 생성
            if (!notificationService.notificationExistsBySender(receiverId, NotificationType.LIKE, senderId)) {
                notificationService.createNotification(
                        receiverId,
                        NotificationType.LIKE,
                        sender.getName() + " 님이 회원님을 좋아합니다.",
                        sender.getName(),
                        senderId
                );
            }

            return new LikeResponse(newLike.getId(), false); // 아직 상호 매칭(요청)은 아님
        }
    }

    /**
     * 좋아요 취소
     */
    @Transactional
    public void cancelLike(Long senderId, Long receiverId) {
        // 매칭 선호도 등록 여부 확인
        validateUserMatchPreference(receiverId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> ServiceException.notFound("전송하는 사용자를 찾을 수 없습니다."));

        Match like = matchRepository.findBySenderIdAndReceiverIdAndMatchType(senderId, receiverId, MatchType.LIKE)
                .orElseThrow(() -> ServiceException.notFound("취소할 '좋아요' 기록이 존재하지 않습니다."));

        // 기존 '좋아요' 알림 삭제
        notificationService.deleteNotificationBySender(receiverId, NotificationType.LIKE, senderId);

        // '좋아요 취소' 알림이 없다면 새로 생성
        if (!notificationService.notificationExistsBySender(receiverId, NotificationType.LIKE_CANCELED, senderId)) {
            notificationService.createNotification(
                    receiverId,
                    NotificationType.LIKE_CANCELED,
                    sender.getName() + " 님이 좋아요를 취소했습니다.",
                    sender.getName(),
                    senderId
            );
        }

        matchRepository.delete(like);
    }

    /**
     * 매칭 참여자 권한 검증
     */
    private void validateMatchParticipant(Match match, Long userId) {
        if (!match.getSender().getId().equals(userId) && !match.getReceiver().getId().equals(userId)) {
            throw ServiceException.forbidden("룸메이트 확정 권한이 없습니다.");
        }
    }

    /**
     * 매칭 타입 전이 처리 및 검증
     */
    private void validateAndHandleMatchTypeTransition(Match match) {
        if (match.getMatchType() == MatchType.LIKE) {
            // LIKE -> REQUEST 전이 처리 (영속 상태에서 자동 flush)
            match.upgradeToRequest(match.getSender(), match.getReceiver());
        } else if (match.getMatchType() != MatchType.REQUEST) {
            // REQUEST가 아닌 다른 타입은 처리 불가
            throw ServiceException.badRequest("요청 상태가 아닌 매칭은 처리할 수 없습니다.");
        }
        // REQUEST 타입은 그대로 진행 (검증만)
    }

}