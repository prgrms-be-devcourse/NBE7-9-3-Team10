package com.unimate.domain.user.admin.service

import com.unimate.domain.user.admin.dto.AdminLoginRequest
import com.unimate.domain.user.admin.dto.AdminSignupRequest
import com.unimate.domain.user.admin.dto.AdminSignupResponse
import com.unimate.domain.user.admin.entity.AdminUser
import com.unimate.domain.user.admin.repository.AdminRepository
import com.unimate.global.auth.dto.Tokens
import com.unimate.global.auth.model.SubjectType
import com.unimate.global.auth.service.TokenService
import com.unimate.global.exception.ServiceException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAuthService(
    private val adminUserRepository: AdminRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val tokenService: TokenService
) {

    @Transactional
    fun signup(request: AdminSignupRequest): AdminSignupResponse {
        if (adminUserRepository.existsByEmail(request.email)) {
            throw ServiceException.badRequest("이미 등록된 관리자 이메일입니다.")
        }

        val admin = AdminUser(
            request.email,
            passwordEncoder.encode(request.password),
            request.name
        )

        val savedAdmin = adminUserRepository.save(admin)

        return AdminSignupResponse(
            requireNotNull(savedAdmin.id) { "저장된 관리자의 ID가 null입니다." },
            savedAdmin.email,
            savedAdmin.name
        )
    }

    @Transactional
    fun login(request: AdminLoginRequest): Tokens {
        val admin = adminUserRepository.findByEmail(request.email)
            ?: throw ServiceException.notFound("관리자를 찾을 수 없습니다.")

        if (!passwordEncoder.matches(request.password, admin.password)) {
            throw ServiceException.unauthorized("비밀번호가 일치하지 않습니다.")
        }

        val adminId = requireNotNull(admin.id) { "관리자 ID가 null입니다." }

        return tokenService.issueTokens(SubjectType.ADMIN, adminId, admin.email)
    }

    @Transactional(readOnly = true)
    fun reissueAccessToken(refreshToken: String): String =
        tokenService.reissueAccessToken(refreshToken)

    @Transactional
    fun logout(refreshToken: String) {
        tokenService.logout(refreshToken)
    }
}