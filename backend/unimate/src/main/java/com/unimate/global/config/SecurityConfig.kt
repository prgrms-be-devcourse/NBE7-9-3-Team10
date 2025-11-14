package com.unimate.global.config

import com.unimate.global.jwt.JwtAuthEntryPoint
import com.unimate.global.jwt.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtFilter: JwtAuthFilter,
    private val entryPoint: JwtAuthEntryPoint
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .headers { it.frameOptions { frame -> frame.disable() } }
            .exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/signup",
                        "/api/v1/auth/token/refresh",
                        "/api/v1/email/**",
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/signup",
                        "/api/v1/admin/auth/token/refresh",
                        "/error",
                        "/favicon.ico",
                        "/h2-console/**",
                        "/ws-stomp/**", // WS 핸드셰이크 허용
                        "/ws-test.html", // 임시용
                        "/v3/api-docs/**",       // <- JSON 문서
                        "/swagger-ui/**",        // <- UI
                        "/swagger-ui.html",      // <- 직접 접근 시
                        "/css/**", "/js/**", "/images/**", "/webjars/**"  // 임시용
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}