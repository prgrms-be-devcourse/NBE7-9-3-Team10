package com.unimate.domain.userProfile.controller

import com.unimate.domain.userProfile.dto.ProfileCreateRequest
import com.unimate.domain.userProfile.dto.ProfileResponse
import com.unimate.domain.userProfile.service.UserProfileService
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "UserProfileController", description = "유저 프로필 API")
@SecurityRequirement(name = "BearerAuth")
class UserProfileController(
    private val userProfileService: UserProfileService
) {

    @PostMapping
    @Operation(summary = "유저 프로필 생성")
    fun createUserProfile(
        @Valid @RequestBody req: ProfileCreateRequest,
        @AuthenticationPrincipal userPrincipal: CustomUserPrincipal
    ): ResponseEntity<ProfileResponse> {
        val response = userProfileService.create(userPrincipal.email, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @Operation(summary = "유저 프로필 조회")
    fun getMyProfile(
        @AuthenticationPrincipal userPrincipal: CustomUserPrincipal
    ): ResponseEntity<ProfileResponse> {
        val response = userProfileService.getByEmail(userPrincipal.email)
        return ResponseEntity.ok(response)
    }

    @PutMapping
    @Operation(summary = "유저 프로필 수정")
    fun updateMyProfile(
        @Valid @RequestBody req: ProfileCreateRequest,
        @AuthenticationPrincipal userPrincipal: CustomUserPrincipal
    ): ResponseEntity<ProfileResponse> {
        val response = userProfileService.update(userPrincipal.email, req)
        return ResponseEntity.ok(response)
    }
}
