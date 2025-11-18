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
import java.util.function.Supplier

@Service
class UserAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val tokenService: TokenService,
    private val verificationService: VerificationService
) {

    @Transactional
    fun signup(req: UserSignupRequest): UserSignupResponse {
        if (userRepository.existsByEmail(req.email)) {
            throw ServiceException.conflict("이미 가입된 이메일입니다.")
        }

        verificationService.assertVerifiedEmailOrThrow(req.email)

        val user = User(
            req.name,
            req.email,
            passwordEncoder.encode(req.password),
            req.gender,
            req.birthDate,
            req.university
        )
        user.verifyStudent()

        val savedUser = userRepository.save(user)
        verificationService.consumeVerification(req.email)

        val userId = requireNotNull(savedUser.id) { "저장된 사용자의 ID가 null입니다." }

        return UserSignupResponse(userId, savedUser.email, savedUser.name)
    }

    @Transactional
    fun login(req: UserLoginRequest): Tokens {
        val user = userRepository.findByEmail(req.email)
            .orElseThrow<ServiceException?>(Supplier { ServiceException.unauthorized("이메일 또는 비밀번호가 일치하지 않습니다.") })

        if (!passwordEncoder.matches(req.password, user.password)) {
            throw ServiceException.unauthorized("이메일 또는 비밀번호가 일치하지 않습니다.")
        }

        return tokenService.issueTokens(SubjectType.USER, user.id!!, user.email)
    }

    @Transactional(readOnly = true)
    fun reissueAccessToken(refreshToken: String): String {
        return tokenService.reissueAccessToken(refreshToken)
    }

    @Transactional
    fun logout(refreshToken: String) {
        tokenService.logout(refreshToken)
    }
}