package com.unimate.domain.user.admin.controller;

import com.unimate.domain.user.admin.dto.AdminLoginRequest;
import com.unimate.domain.user.admin.dto.AdminLoginResponse;
import com.unimate.domain.user.admin.dto.AdminSignupRequest;
import com.unimate.domain.user.admin.dto.AdminSignupResponse;
import com.unimate.domain.user.admin.service.AdminAuthService;
import com.unimate.domain.user.admin.service.AdminService;
import com.unimate.global.auth.dto.AccessTokenResponse;
import com.unimate.global.auth.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.unimate.global.util.CookieUtilsKt.expireCookie;
import static com.unimate.global.util.CookieUtilsKt.httpOnlyCookie;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "AdminAuthController", description = "관리자 인증인가 API")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminService adminUserService;

    @Value("${auth.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${auth.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/signup")
    @Operation(summary = "관리자 회원가입")
    public ResponseEntity<AdminSignupResponse> signup(@Valid @RequestBody AdminSignupRequest request) {
        return ResponseEntity.ok(adminAuthService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        var tokens = adminAuthService.login(request);

        ResponseCookie cookie = httpOnlyCookie(
                "adminRefreshToken",
                tokens.getRefreshToken(),
                7L * 24 * 60 * 60,
                cookieSecure,
                cookieSameSite
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AdminLoginResponse(tokens.getSubjectId(), tokens.getEmail(), tokens.getAccessToken()));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "관리자 토큰 재발급")
    public ResponseEntity<AccessTokenResponse> refreshToken(
            @CookieValue(name = "adminRefreshToken", required = false) String refreshToken
    ) {
        String newAccessToken = adminAuthService.reissueAccessToken(refreshToken);
        return ResponseEntity.ok(new AccessTokenResponse(newAccessToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "관리자 로그아웃", security = { @SecurityRequirement(name = "BearerAuth") })
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "adminRefreshToken", required = false) String refreshToken
    ) {
        adminAuthService.logout(refreshToken);
        ResponseCookie expired = expireCookie("adminRefreshToken", cookieSecure, cookieSameSite);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .body(new MessageResponse("관리자 로그아웃이 완료되었습니다."));
    }
}
