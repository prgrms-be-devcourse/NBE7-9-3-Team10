package com.unimate.domain.verification.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailVerificationRequest(
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String
)