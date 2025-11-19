package com.unimate.domain.review.service

import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.global.exception.ServiceException
import org.springframework.stereotype.Service

/**
 * 재매칭 관련 비즈니스 로직 담당 서비스
 * - Match 엔티티를 기준으로 재매칭 조건을 검사하는 역할
 * - ReviewService와 연동해서 '후기 기반 재매칭 여부' 확인 가능
 */
@Service
class MatchRematchService(
    private val reviewService: ReviewService
) {

    /**
     * 재매칭 가능 여부
     * - 매칭 상태가 ACCEPTED
     * - 첫 매칭 (rematchRound = 0)
     */
    fun canRematchBasic(match: Match): Boolean {
        return match.matchStatus == MatchStatus.ACCEPTED && match.rematchRound == 0
    }

    /**
     * 재매칭 가능 여부 확인 (후기 작성 없이도 가능)
     */
    fun canRematchWithoutReview(match: Match): Boolean {
        return canRematchBasic(match)
    }

    /**
     * 재매칭 가능 여부 확인 (Review 기반)
     * - 기본 조건 + 양쪽 모두 후기 작성 + 양쪽 모두 재매칭 동의
     */
    fun canRematchBasedOnReviews(match: Match): Boolean {
        // 기본 조건 확인
        if (!canRematchBasic(match)) {
            return false
        }
        // 후기 기반 조건 확인
        return reviewService.canRematchBasedOnReviews(match)
    }

    /**
     * 재매칭 통합 조건 체크
     */
    fun canRematch(match: Match, requireReview: Boolean = false): Boolean {
        return if (requireReview) {
            canRematchBasedOnReviews(match)
        } else {
            canRematchWithoutReview(match)
        }
    }

    /**
     * 재매칭 요청 시 + 다음 회차 계산
     * - 조건 안 맞으면 상세 이유와 함께 예외 발생
     */
    fun validateAndGetNextRound(match: Match, requireReview: Boolean = false): Int {
        if (!canRematch(match, requireReview)) {
            val reason = when {
                match.matchStatus != MatchStatus.ACCEPTED -> "매칭이 성사되지 않았습니다."
                match.rematchRound > 0 -> "이미 재매칭된 매칭입니다."
                requireReview && !reviewService.hasBothReviews(match) -> "양방향 후기 작성이 필요합니다."
                requireReview && !reviewService.canRematch(match) -> "양쪽 모두 재매칭 동의가 필요합니다."
                else -> "재매칭이 불가능한 상태입니다."
            }
            throw ServiceException.badRequest(reason)
        }

        return match.getNextRematchRound()
    }
}