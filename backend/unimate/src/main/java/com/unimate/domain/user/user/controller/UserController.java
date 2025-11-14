package com.unimate.domain.user.user.controller;

import com.unimate.domain.user.user.dto.*;
import com.unimate.domain.user.user.entity.User;
import com.unimate.domain.user.user.service.UserService;
import com.unimate.global.jwt.CustomUserPrincipal;
import com.unimate.global.jwt.JwtProvider;
import com.unimate.global.jwt.JwtToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Validated
@Tag(name = "UserAuthController", description = "유저 정보 API")
@SecurityRequirement(name = "BearerAuth")
public class UserController {
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @GetMapping
    @Operation(summary = "유저 정보 조회")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        User user = userService.findByEmail(userPrincipal.email);
        return ResponseEntity.ok(
                new UserInfoResponse(
                        user.getEmail(),
                        user.getName(),
                        user.getGender(),
                        user.getBirthDate(),
                        user.getUniversity()
                ));
    }

    @PatchMapping("/name")
    @Operation(summary = "유저 이름 수정")
    public ResponseEntity<UserUpdateResponse> updateUserName(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestBody UserUpdateNameRequest request
            ) {
        User user = userService.updateName(userPrincipal.email, request.getName());
        return ResponseEntity.ok(
                new UserUpdateResponse(
                        user.getEmail(),
                        user.getName(),
                        user.getGender(),
                        user.getBirthDate(),
                        user.getUniversity()
                ));
    }


    @PatchMapping("/email")
    @Operation(summary = "유저 이메일 수정")
    public ResponseEntity<UserUpdateEmailResponse> updateUserEmail(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody UserUpdateEmailRequest request
    ) {
        User updated = userService.updateEmail(userPrincipal.email, request);

        JwtToken newToken = jwtProvider.generateToken(updated.getEmail(), updated.getId());

        return ResponseEntity.ok(
                new UserUpdateEmailResponse(
                        updated.getEmail(),
                        updated.getName(),
                        updated.getGender(),
                        updated.getBirthDate(),
                        updated.getUniversity(),
                        newToken.getAccessToken()
                ));
    }
}
