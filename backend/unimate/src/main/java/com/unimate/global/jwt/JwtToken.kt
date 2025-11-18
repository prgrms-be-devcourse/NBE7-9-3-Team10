package com.unimate.global.jwt

class JwtToken(
    var grantType: String,
    var accessToken: String,
    var refreshToken: String,
    var accessTokenExpiresIn: Long
)
