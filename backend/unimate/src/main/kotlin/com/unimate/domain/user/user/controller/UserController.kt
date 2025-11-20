package com.unimate.domain.user.user.controller

import com.unimate.domain.user.user.dto.*
import com.unimate.domain.user.user.service.UserService
import com.unimate.global.jwt.CustomUserPrincipal
import com.unimate.global.jwt.JwtProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user")
@Validated
@Tag(name = "UserController", description = "유저 정보 API")
@SecurityRequirement(name = "BearerAuth")
class UserController(
    private val userService: UserService,
    private val jwtProvider: JwtProvider
) {

    @GetMapping
    @Operation(summary = "유저 정보 조회")
    fun getUserInfo(@AuthenticationPrincipal userPrincipal: CustomUserPrincipal): ResponseEntity<UserInfoResponse> {
        val user = userService.findByEmail(userPrincipal.email)
        return ResponseEntity.ok(
            UserInfoResponse(
                user.email,
                user.name,
                user.gender,
                user.birthDate,
                user.university
            )
        )
    }

    @PatchMapping("/name")
    @Operation(summary = "유저 이름 수정")
    fun updateUserName(
        @AuthenticationPrincipal userPrincipal: CustomUserPrincipal,
        @Valid @RequestBody request: UserUpdateNameRequest
    ): ResponseEntity<UserUpdateResponse> {
        val user = userService.updateName(userPrincipal.email, request.name)
        return ResponseEntity.ok(
            UserUpdateResponse(
                user.email,
                user.name,
                user.gender,
                user.birthDate,
                user.university
            )
        )
    }

    @GetMapping("/email/domain")
    @Operation(summary = "현재 대학교의 이메일 도메인 조회")
    fun getEmailDomain(@AuthenticationPrincipal userPrincipal: CustomUserPrincipal): ResponseEntity<EmailDomainResponse> {
        val user = userService.findByEmail(userPrincipal.email)
        val domain = userService.getUniversityDomain(user.university)
            ?: throw com.unimate.global.exception.ServiceException.badRequest(
                "${user.university}의 도메인을 찾을 수 없습니다."
            )

        return ResponseEntity.ok(
            EmailDomainResponse(
                university = user.university,
                domain = domain,
                currentEmail = user.email
            )
        )
    }

    @PatchMapping("/email")
    @Operation(summary = "유저 이메일 수정 (prefix + code)")
    fun updateUserEmail(
        @AuthenticationPrincipal userPrincipal: CustomUserPrincipal,
        @Valid @RequestBody request: UserUpdateEmailRequest
    ): ResponseEntity<UserUpdateEmailResponse> {
        val updated = userService.updateEmail(userPrincipal.email, request)
        val userId = requireNotNull(updated.id) { "User id가 null입니다" }
        val newToken = jwtProvider.generateToken(updated.email, userId)

        return ResponseEntity.ok(
            UserUpdateEmailResponse(
                updated.email,
                updated.name,
                updated.gender,
                updated.birthDate,
                updated.university,
                newToken.accessToken
            )
        )
    }
}