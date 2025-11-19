package com.unimate.domain.user.user.dto

data class UserSignupResponse(
    val userId: Long,
    val email: String,
    val name: String
)