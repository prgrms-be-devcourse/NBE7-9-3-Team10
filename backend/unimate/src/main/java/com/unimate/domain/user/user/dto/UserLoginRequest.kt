package com.unimate.domain.user.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserLoginRequest(
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    var email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    var password: String
)