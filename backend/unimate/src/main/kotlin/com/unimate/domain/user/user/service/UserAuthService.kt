package com.unimate.domain.user.user.service

import com.unimate.domain.user.user.dto.UserLoginRequest
import com.unimate.domain.user.user.dto.UserSignupRequest
import com.unimate.domain.user.user.dto.UserSignupResponse
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.verification.service.VerificationService
import com.unimate.global.auth.dto.Tokens
import com.unimate.global.auth.model.SubjectType
import com.unimate.global.auth.service.TokenService
import com.unimate.global.exception.ServiceException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val tokenService: TokenService,
    private val verificationService: VerificationService
) {

    @Transactional
    fun signup(request: UserSignupRequest): UserSignupResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ServiceException.conflict("이미 가입된 이메일입니다.")
        }

        verificationService.assertVerifiedEmailOrThrow(request.email)

        val user = User(
            request.name,
            request.email,
            passwordEncoder.encode(request.password),
            request.gender,
            request.birthDate,
            request.university
        )
        user.studentVerified = true

        val savedUser = userRepository.save(user)
        verificationService.consumeVerification(request.email)


        return UserSignupResponse(
            savedUser.id ?: throw ServiceException.internalServerError("저장된 사용자의 ID가 null입니다."),
            savedUser.email,
            savedUser.name
        )
    }

    @Transactional
    fun login(request: UserLoginRequest): Tokens {
        val user = userRepository.findByEmail(request.email)
            ?: throw ServiceException.unauthorized("이메일이 일치하지 않습니다.")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ServiceException.unauthorized("비밀번호가 일치하지 않습니다.")
        }

        return tokenService.issueTokens(
            SubjectType.USER,
            user.id ?: throw ServiceException.internalServerError("사용자 ID가 null입니다."),
            user.email
        )
    }

    @Transactional(readOnly = true)
    fun reissueAccessToken(refreshToken: String): String =
        tokenService.reissueAccessToken(refreshToken)

    @Transactional
    fun logout(refreshToken: String) {
        tokenService.logout(refreshToken)
    }
}