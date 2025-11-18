package com.unimate.domain.user.admin.controller

import com.unimate.domain.user.admin.dto.AdminLoginRequest
import com.unimate.domain.user.admin.dto.AdminLoginResponse
import com.unimate.domain.user.admin.dto.AdminSignupRequest
import com.unimate.domain.user.admin.dto.AdminSignupResponse
import com.unimate.domain.user.admin.service.AdminAuthService
import com.unimate.global.auth.dto.AccessTokenResponse
import com.unimate.global.auth.dto.MessageResponse
import com.unimate.global.util.expireCookie
import com.unimate.global.util.httpOnlyCookie
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "AdminAuthController", description = "관리자 인증인가 API")
class AdminAuthController(
    private val adminAuthService: AdminAuthService,

    @Value("\${auth.cookie.secure:false}")
    private val cookieSecure: Boolean,

    @Value("\${auth.cookie.same-site:Lax}")
    private val cookieSameSite: String
) {

    @PostMapping("/signup")
    @Operation(summary = "관리자 회원가입")
    fun signup(@Valid @RequestBody request: AdminSignupRequest): ResponseEntity<AdminSignupResponse> =
        ResponseEntity.ok(adminAuthService.signup(request))

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인")
    fun login(@Valid @RequestBody request: AdminLoginRequest): ResponseEntity<AdminLoginResponse> {
        val tokens = adminAuthService.login(request)

        val cookie = httpOnlyCookie(
            ADMIN_REFRESH_TOKEN_NAME,
            tokens.refreshToken,
            TOKEN_EXPIRY_SECONDS,
            cookieSecure,
            cookieSameSite
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(AdminLoginResponse(tokens.subjectId, tokens.email, tokens.accessToken))
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "관리자 토큰 재발급")
    fun refreshToken(
        @CookieValue(name = ADMIN_REFRESH_TOKEN_NAME, required = true) refreshToken: String
    ): ResponseEntity<AccessTokenResponse> =
        ResponseEntity.ok(AccessTokenResponse(adminAuthService.reissueAccessToken(refreshToken)))


    @PostMapping("/logout")
    @Operation(summary = "관리자 로그아웃", security = [SecurityRequirement(name = "BearerAuth")])
    fun logout(
        @CookieValue(name = ADMIN_REFRESH_TOKEN_NAME, required = true) refreshToken: String
    ): ResponseEntity<MessageResponse> {
        adminAuthService.logout(refreshToken)
        val expired = expireCookie(ADMIN_REFRESH_TOKEN_NAME, cookieSecure, cookieSameSite)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expired.toString())
            .body(MessageResponse("관리자 로그아웃이 완료되었습니다."))
    }

    companion object {
        private const val ADMIN_REFRESH_TOKEN_NAME = "adminRefreshToken"
        private const val TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60
    }
}