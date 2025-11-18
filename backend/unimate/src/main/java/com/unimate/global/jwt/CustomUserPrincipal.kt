package com.unimate.global.jwt

import java.security.Principal

class CustomUserPrincipal(
    val userId: Long,
    val email: String
) : Principal {

    override fun getName(): String = userId.toString()

    override fun toString(): String = name
}