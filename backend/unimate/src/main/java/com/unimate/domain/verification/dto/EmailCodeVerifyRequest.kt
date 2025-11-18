package com.unimate.domain.verification.dto

import jakarta.validation.constraints.NotBlank

data class EmailCodeVerifyRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:NotBlank(message = "인증코드는 필수입니다.")
    val code: String
)