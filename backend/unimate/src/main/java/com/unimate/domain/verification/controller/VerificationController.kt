package com.unimate.domain.verification.controller

import com.unimate.domain.verification.dto.EmailCodeVerifyRequest
import com.unimate.domain.verification.dto.EmailVerificationRequest
import com.unimate.domain.verification.dto.SchoolDomainResponse
import com.unimate.domain.verification.dto.SchoolListResponse
import com.unimate.domain.verification.service.VerificationService
import com.unimate.global.auth.dto.MessageResponse
import com.unimate.global.school.service.SchoolService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/email")
@Validated
@Tag(name = "VerificationController", description = "이메일 인증 API")
class VerificationController(
    private val verificationService: VerificationService,
    private val schoolService: SchoolService
) {

    @GetMapping("/all-schools")
    @Operation(summary = "전체 학교 목록 조회")
    fun getAllSchools(): ResponseEntity<List<SchoolListResponse>> {
        val schools = schoolService.getAllSchools()
        return ResponseEntity.ok(schools)
    }

    @GetMapping("/school-domain")
    @Operation(summary = "학교명으로 도메인 조회")
    fun getSchoolDomain(@RequestParam schoolName: String): ResponseEntity<SchoolDomainResponse> {
        val domain = schoolService.getPrimarySchoolDomain(schoolName)

        return if (domain != null) {
            ResponseEntity.ok(SchoolDomainResponse(domain))
        } else {
            ResponseEntity.badRequest().body(SchoolDomainResponse(null))
        }
    }

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