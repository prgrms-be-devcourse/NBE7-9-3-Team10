package com.unimate.domain.user.admin.dto

data class AdminLoginResponse (
    val adminId: Long,
    val email: String,
    val accessToken: String
)