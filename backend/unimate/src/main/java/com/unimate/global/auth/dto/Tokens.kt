package com.unimate.global.auth.dto

data class Tokens(
    val subjectId: Long,
    val email: String,
    val accessToken: String,
    val refreshToken: String
) {
    companion object {
        @JvmStatic
        fun of(subjectId: Long, email: String, accessToken: String, refreshToken: String): Tokens {
            return Tokens(
                subjectId,
                email,
                accessToken,
                refreshToken
            )
        }
    }
}

