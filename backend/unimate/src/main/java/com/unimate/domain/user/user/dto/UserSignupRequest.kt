package com.unimate.domain.user.user.dto

import com.unimate.domain.user.user.entity.Gender
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UserSignupRequest(
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    var email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    var password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    var name: String,

    @field:NotNull(message = "성별은 필수입니다.")
    var gender: Gender,

    @field:NotNull(message = "생년월일은 필수입니다.")
    var birthDate: LocalDate,

    @field:NotBlank(message = "대학교는 필수입니다.")
    var university:  String
)

