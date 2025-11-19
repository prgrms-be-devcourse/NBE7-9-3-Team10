package com.unimate.global.jwt

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthEntryPoint : AuthenticationEntryPoint {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthEntryPoint::class.java)
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        log.error("Unauthorized: {}", authException.message)
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
    }
}