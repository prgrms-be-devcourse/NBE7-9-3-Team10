package com.unimate.global.util

import org.springframework.http.ResponseCookie

fun httpOnlyCookie(
    name: String,
    value: String,
    maxAgeSeconds: Long,
    secure: Boolean,
    sameSite: String
): ResponseCookie {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .sameSite(sameSite)
        .maxAge(maxAgeSeconds)
        .build()
}

fun expireCookie(
    name: String,
    secure: Boolean,
    sameSite: String
): ResponseCookie {
    return httpOnlyCookie(name, "", 0, secure, sameSite)
}