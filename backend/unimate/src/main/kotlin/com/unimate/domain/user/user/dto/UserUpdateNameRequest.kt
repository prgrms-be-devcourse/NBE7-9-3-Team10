package com.unimate.domain.user.user.dto

import jakarta.validation.constraints.NotBlank

data class UserUpdateNameRequest (
    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String
)