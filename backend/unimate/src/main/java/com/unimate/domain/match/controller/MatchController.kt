package com.unimate.domain.match.controller

import com.unimate.domain.match.dto.*
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.service.MatchService
import com.unimate.domain.match.service.MatchUtilityService
import com.unimate.global.exception.ServiceException
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "MatchController", description = "매칭 API")
@SecurityRequirement(name = "BearerAuth")
class MatchController(
    private val matchService: MatchService,
    private val matchUtilityService: MatchUtilityService
) {

    /**
     * 룸메이트 추천 목록 조회 (필터 적용)
     */
    @GetMapping("/recommendations")
    @Operation(summary = "룸메이트 추천 목록 조회")
    fun getMatchRecommendations(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @Valid request: MatchRecommendationRequest
    ): ResponseEntity<MatchRecommendationResponse> {
        val response = matchService.getMatchRecommendations(
            user.email,
            request.sleepPattern,
            request.ageRange,
            request.cleaningFrequency,
            request.startDate,
            request.endDate
        )
        return ResponseEntity.ok(response)
    }

    /**
     * 후보 프로필 상세 조회
     */
    @GetMapping("/candidates/{receiverId}")
    @Operation(summary = "룸메이트 후보 프로필 상세 조회")
    fun getMatchRecommendationDetail(
        @PathVariable receiverId: Long,
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<MatchRecommendationDetailResponse> {
        val response = matchService.getMatchRecommendationDetail(user.email, receiverId)
        return ResponseEntity.ok(response)
    }

    /**
     * 룸메이트 최종 확정/거절
     */
    @PutMapping("/{id}/confirm")
    @Operation(summary = "룸메이트 최종 확정/거절")
    fun confirmMatch(
        @PathVariable id: Long,
        @Valid @RequestBody request: MatchConfirmRequest,
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<MatchConfirmResponse> {
        return when (request.action) {
            "accept" -> {
                val match = matchService.confirmMatch(id, user.userId)
                ResponseEntity.ok(buildMatchConfirmResponse(match, "룸메이트 매칭이 최종 확정되었습니다."))
            }
            "reject" -> {
                val match = matchService.rejectMatch(id, user.userId)
                ResponseEntity.ok(buildMatchConfirmResponse(match, "룸메이트 매칭이 거절되었습니다."))
            }
            else -> ResponseEntity.badRequest().build()
        }
    }

    /**
     * 매칭 상태 조회
     * 사용자의 모든 매칭 상태를 조회 (PENDING, ACCEPTED, REJECTED)
     */
    @GetMapping("/status")
    @Operation(summary = "룸메이트 매칭 상태 조회")
    fun getMatchStatus(
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<MatchStatusResponse> {
        val response = matchService.getMatchStatus(user.userId)
        return ResponseEntity.ok(response)
    }

    /**
     * 룸메이트 성사 결과
     * 성사된 매칭 결과만 조회 (ACCEPTED 상태)
     */
    @GetMapping("/results")
    @Operation(summary = "룸메이트 성사 결과 조회")
    fun getMatchResults(
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<MatchResultResponse> {
        val response = matchService.getMatchResults(user.userId)
        return ResponseEntity.ok(response)
    }

    /**
     * 좋아요 보내기
     */
    @PostMapping("/likes")
    @Operation(summary = "룸메이트 좋아요 보내기")
    fun sendLike(
        @Valid @RequestBody requestDto: LikeRequest,
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<LikeResponse> {
        val response = matchService.sendLike(requestDto, user.userId)
        return ResponseEntity.ok(response)
    }

    /**
     * 좋아요 취소
     */
    @DeleteMapping("/{receiverId}")
    @Operation(summary = "룸메이트 좋아요 취소")
    fun cancelLike(
        @PathVariable receiverId: Long,
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<Void> {
        matchService.cancelLike(user.userId, receiverId)
        return ResponseEntity.noContent().build()
    }


    // 매칭 확정 응답 생성 헬퍼 메서드
    private fun buildMatchConfirmResponse(
        match: Match,
        message: String
    ): MatchConfirmResponse {
        val matchId = match.id ?: throw ServiceException.internalServerError("매칭 ID가 null입니다.")
        val senderId = match.sender.id ?: throw ServiceException.internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw ServiceException.internalServerError("수신자 ID가 null입니다.")


        val senderInfo = MatchConfirmResponse.SenderInfo(
            senderId,
            match.sender.name,
            match.sender.email,
            match.sender.university
        )

        val receiverInfo = MatchConfirmResponse.ReceiverInfo(
            receiverId,
            match.receiver.name,
            matchUtilityService.calculateAge(match.receiver.birthDate),
            match.receiver.university,
            match.receiver.email
        )

        return MatchConfirmResponse(
            matchId,
            senderId,
            receiverId,
            match.matchType,
            match.matchStatus,
            match.preferenceScore,
            match.confirmedAt,
            message,
            senderInfo,
            receiverInfo
        )
    }
}
