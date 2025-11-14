package com.unimate.domain.verification.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class EmailCodeVerifyRequest(
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:Pattern(regexp = "^[0-9]{6}$", message = "인증코드는 6자리 숫자입니다.")
    val code: String
)