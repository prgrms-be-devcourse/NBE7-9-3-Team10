package com.unimate.domain.user.user.controller

import com.unimate.domain.user.user.dto.UserLoginRequest
import com.unimate.domain.user.user.dto.UserLoginResponse
import com.unimate.domain.user.user.dto.UserSignupRequest
import com.unimate.domain.user.user.dto.UserSignupResponse
import com.unimate.domain.user.user.service.UserAuthService
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
@RequestMapping("/api/v1/auth")
@Tag(name = "UserAuthController", description = "유저 인증인가 API")
class UserAuthController(
    private val userAuthService: UserAuthService
) {

    @Value("\${auth.cookie.secure:false}")
    private val cookieSecure = false

    @Value("\${auth.cookie.same-site:Lax}")
    private val cookieSameSite: String = "Lax"

    @PostMapping("/signup")
    @Operation(summary = "유저 회원가입")
    fun signup(@RequestBody request: @Valid UserSignupRequest): ResponseEntity<UserSignupResponse> {
        return ResponseEntity.ok(userAuthService.signup(request))
    }

    @PostMapping("/login")
    @Operation(summary = "유저 로그인")
    fun login(@RequestBody request: @Valid UserLoginRequest): ResponseEntity<UserLoginResponse> {
        val tokens = userAuthService.login(request)

        val cookie = httpOnlyCookie(
            "refreshToken",
            tokens.refreshToken,
            7L * 24 * 60 * 60,
            cookieSecure,
            cookieSameSite
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(UserLoginResponse(tokens.subjectId, tokens.email, tokens.accessToken))
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "유저 토큰 재발급")
    fun refreshToken(
        @CookieValue(name = "refreshToken", required = true) refreshToken: String
    ): ResponseEntity<AccessTokenResponse> {
        val newAccessToken = userAuthService.reissueAccessToken(refreshToken)
        return ResponseEntity.ok(AccessTokenResponse(newAccessToken))
    }

    @PostMapping("/logout")
    @Operation(summary = "유저 로그아웃", security = [SecurityRequirement(name = "BearerAuth")])
    fun logout(
        @CookieValue(name = "refreshToken", required = true) refreshToken: String
    ): ResponseEntity<MessageResponse> {
        userAuthService.logout(refreshToken)
        val expired = expireCookie("refreshToken", cookieSecure, cookieSameSite)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expired.toString())
            .body(MessageResponse("로그아웃이 완료되었습니다."))
    }
}