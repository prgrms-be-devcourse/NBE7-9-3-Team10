package com.unimate.domain.match.controller;

import com.unimate.domain.match.dto.*;
import com.unimate.domain.match.entity.Match;
import com.unimate.domain.match.service.MatchService;
import com.unimate.domain.match.service.MatchUtilityService;
import com.unimate.global.jwt.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "MatchController", description = "매칭 API")
@SecurityRequirement(name = "BearerAuth")
public class MatchController {

    private final MatchService matchService;
    private final MatchUtilityService matchUtilityService;

    /**
     * 룸메이트 추천 목록 조회 (필터 적용)
     */
    @GetMapping("/recommendations")
    @Operation(summary = "룸메이트 추천 목록 조회")
    public ResponseEntity<MatchRecommendationResponse> getMatchRecommendations(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @Valid MatchRecommendationRequest request
    ) {
        MatchRecommendationResponse response = matchService.getMatchRecommendations(
                user.getEmail(),
                request.getSleepPattern(),
                request.getAgeRange(),
                request.getCleaningFrequency(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 후보 프로필 상세 조회
     */
    @GetMapping("/candidates/{receiverId}")
    @Operation(summary = "룸메이트 후보 프로필 상세 조회")
    public ResponseEntity<MatchRecommendationDetailResponse> getMatchRecommendationDetail(
            @PathVariable("receiverId") Long receiverId,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        MatchRecommendationDetailResponse response =
                matchService.getMatchRecommendationDetail(user.getEmail(), receiverId);
        return ResponseEntity.ok(response);
    }

    /**
     * 룸메이트 최종 확정/거절
     */
    @PutMapping("/{id}/confirm")
    @Operation(summary = "룸메이트 최종 확정/거절")
    public ResponseEntity<MatchConfirmResponse> confirmMatch(
            @PathVariable("id") Long id,
            @Valid @RequestBody MatchConfirmRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        String action = request.getAction();
        if (action == null) {
            return ResponseEntity.badRequest().build();
        }

        return switch (request.getAction()) {
            case "accept" -> {
                Match match = matchService.confirmMatch(id, user.getUserId());
                yield ResponseEntity.ok(buildMatchConfirmResponse(match, "룸메이트 매칭이 최종 확정되었습니다."));
            }
            case "reject" -> {
                Match match = matchService.rejectMatch(id, user.getUserId());
                yield ResponseEntity.ok(buildMatchConfirmResponse(match, "룸메이트 매칭이 거절되었습니다."));
            }
            default -> ResponseEntity.badRequest().build();
        };
    }

    /**
     * 매칭 상태 조회
     * 사용자의 모든 매칭 상태를 조회 (PENDING, ACCEPTED, REJECTED)
     */
    @GetMapping("/status")
    @Operation(summary = "룸메이트 매칭 상태 조회")
    public ResponseEntity<MatchStatusResponse> getMatchStatus(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        MatchStatusResponse response = matchService.getMatchStatus(user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 룸메이트 성사 결과
     * 성사된 매칭 결과만 조회 (ACCEPTED 상태)
     */
    @GetMapping("/results")
    @Operation(summary = "룸메이트 룸메이트 성사 결과 조회")
    public ResponseEntity<MatchResultResponse> getMatchResults(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        MatchResultResponse response = matchService.getMatchResults(user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 좋아요 보내기
     */
    @PostMapping("/likes")
    @Operation(summary = "룸메이트 좋아요 보내기")
    public ResponseEntity<LikeResponse> sendLike(
            @Valid @RequestBody LikeRequest requestDto,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        LikeResponse response = matchService.sendLike(requestDto, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 좋아요 취소
     */
    @DeleteMapping("/{receiverId}")
    @Operation(summary = "룸메이트 좋아요 취소")
    public ResponseEntity<Void> cancelLike(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        matchService.cancelLike(user.getUserId(), receiverId);
        return ResponseEntity.noContent().build();
    }


    // 매칭 확정 응답 생성 헬퍼 메서드
    private MatchConfirmResponse buildMatchConfirmResponse(Match match, String message) {
        MatchConfirmResponse.SenderInfo senderInfo = new MatchConfirmResponse.SenderInfo(
                match.getSender().getId(),
                match.getSender().getName(),
                match.getSender().getEmail(),
                match.getSender().getUniversity()
        );

        MatchConfirmResponse.ReceiverInfo receiverInfo = new MatchConfirmResponse.ReceiverInfo(
                match.getReceiver().getId(),
                match.getReceiver().getName(),
                matchUtilityService.calculateAge(match.getReceiver().getBirthDate()),
                match.getReceiver().getUniversity(),
                match.getReceiver().getEmail()
        );

        return new MatchConfirmResponse(
                match.getId(),
                match.getSender().getId(),
                match.getReceiver().getId(),
                match.getMatchType(),
                match.getMatchStatus(),
                match.getPreferenceScore(),
                match.getConfirmedAt(),
                message,
                senderInfo,
                receiverInfo
        );
    }
}
