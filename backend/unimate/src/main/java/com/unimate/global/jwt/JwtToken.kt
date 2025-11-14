package com.unimate.global.jwt

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor

@Getter
@NoArgsConstructor
@AllArgsConstructor
class JwtToken(
    var grantType: String,
    var accessToken: String,
    var refreshToken: String,
    var accessTokenExpiresIn: Long
)
