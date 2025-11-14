package com.unimate.global.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider {

    private val log = LoggerFactory.getLogger(JwtProvider::class.java)

    @Value("\${jwt.secret}")
    private lateinit var secretKey: String

    @Value("\${jwt.access-token-expiration}")
    private var accessTokenExpirationTime: Long = 0

    @Value("\${jwt.refresh-token-expiration}")
    private var refreshTokenExpirationTime: Long = 0

    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateToken(email: String, userId: Long): JwtToken {
        val now = Date()

        val accessToken = Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpirationTime))
            .signWith(key)
            .compact()

        val refreshToken = Jwts.builder()
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenExpirationTime))
            .signWith(key)
            .compact()

        return JwtToken("Bearer", accessToken, refreshToken, accessTokenExpirationTime)
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            return true
        } catch (e: ExpiredJwtException) {
            log.warn("토큰 만료: {}", e.message)
        } catch (e: JwtException) {
            log.warn("잘못된 토큰: {}", e.message)
        } catch (e: IllegalArgumentException) {
            log.warn("잘못된 토큰: {}", e.message)
        }
        return false
    }

    fun getEmailFromToken(token: String): String {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        return claims.subject
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        val userId = claims.get("userId", Number::class.java)
        return userId.toLong()
    }
}