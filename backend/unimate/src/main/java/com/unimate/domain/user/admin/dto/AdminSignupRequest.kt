package com.unimate.domain.user.admin.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank


data class AdminSignupRequest(
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    var email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    var password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    var name: String
)
