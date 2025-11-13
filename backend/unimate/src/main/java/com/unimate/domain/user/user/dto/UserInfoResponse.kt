package com.unimate.domain.user.user.dto

import com.unimate.domain.user.user.entity.Gender
import java.time.LocalDate

data class UserInfoResponse(
    val email: String,
    val name: String,
    val gender: Gender,
    val birthDate: LocalDate,
    val university: String
)
