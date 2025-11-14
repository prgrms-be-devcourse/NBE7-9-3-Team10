package com.unimate.domain.verification.controller

import com.unimate.domain.verification.dto.EmailCodeVerifyRequest
import com.unimate.domain.verification.dto.EmailVerificationRequest
import com.unimate.domain.verification.service.VerificationService
import com.unimate.global.auth.dto.MessageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/email")
@Validated
@Tag(name = "VerificationController", description = "이메일 인증 API")
class VerificationController(
    private val verificationService: VerificationService
) {

    @PostMapping("/request")
    @Operation(summary = "이메일 인증번호 전송")
    fun request(@Valid @RequestBody request: EmailVerificationRequest): ResponseEntity<MessageResponse> {
        verificationService.sendVerificationCode(request.email)
        return ResponseEntity.ok(MessageResponse("인증코드가 발송되었습니다."))
    }

    @PostMapping("/verify")
    @Operation(summary = "이메일 인증번호 검증")
    fun verify(@Valid @RequestBody request: EmailCodeVerifyRequest): ResponseEntity<MessageResponse> {
        verificationService.verifyCode(request.email, request.code)
        return ResponseEntity.ok(MessageResponse("이메일 인증이 완료되었습니다."))
    }
}