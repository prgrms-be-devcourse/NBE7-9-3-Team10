package com.unimate.domain.userMatchPreference.controller

import com.unimate.domain.userMatchPreference.dto.MatchPreferenceRequest
import com.unimate.domain.userMatchPreference.dto.MatchPreferenceResponse
import com.unimate.domain.userMatchPreference.service.UserMatchPreferenceService
import com.unimate.domain.userProfile.service.UserProfileService
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "UserMatchPreferenceController", description = "유저 매칭 선호도 API")
@SecurityRequirement(name = "BearerAuth")
class UserMatchPreferenceController(
    private val userMatchPreferenceService: UserMatchPreferenceService,
    private val userProfileService: UserProfileService
) {

    @PutMapping("/me/preferences")
    @Operation(summary = "유저 매칭 선호도 수정")
    fun updateMyMatchPreference(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @Valid @RequestBody requestDto: MatchPreferenceRequest
    ): ResponseEntity<MatchPreferenceResponse> {
        val userId = user.userId
        val responseDto = userMatchPreferenceService.updateMyMatchPreferences(userId, requestDto)
        return ResponseEntity.ok(responseDto)
    }

    @DeleteMapping("/me/matching-status")
    @Operation(summary = "유저 매칭 선호도 삭제")
    fun cancelMatchingStatus(@AuthenticationPrincipal user: CustomUserPrincipal): ResponseEntity<Void> {
        userProfileService.cancelMatching(user.userId)
        return ResponseEntity.noContent().build()
    }
}
