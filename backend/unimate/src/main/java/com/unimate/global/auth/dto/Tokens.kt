package com.unimate.global.auth.dto

data class Tokens(
    val subjectId: Long,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)

