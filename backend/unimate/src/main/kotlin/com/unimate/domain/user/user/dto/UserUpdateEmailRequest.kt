package com.unimate.domain.user.user.dto

import jakarta.validation.constraints.NotBlank

data class UserUpdateEmailRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    val emailPrefix: String,

    @field:NotBlank(message = "인증 코드는 필수입니다.")
    val code: String
)
