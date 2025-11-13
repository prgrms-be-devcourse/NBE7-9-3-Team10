package com.unimate.domain.user.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserUpdateEmailRequest(
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val newEmail: String,

    @field:NotBlank(message = "인증 코드는 필수입니다.")
    val code: String
)
