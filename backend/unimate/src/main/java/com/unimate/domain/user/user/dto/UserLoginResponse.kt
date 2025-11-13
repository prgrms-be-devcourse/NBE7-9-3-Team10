package com.unimate.domain.user.user.dto

data class UserLoginResponse(
    val userId: Long,
    val email: String,
    val accessToken: String
)