package com.unimate.domain.user.user.service

import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.dto.UserUpdateEmailRequest
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.verification.repository.VerificationRepository
import com.unimate.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val verificationRepository: VerificationRepository,
    private val matchCacheService: MatchCacheService
) {
    companion object {
        private val log = LoggerFactory.getLogger(UserService::class.java)
    }

    @Transactional
    fun findByEmail(email: String): User =
        userRepository.findByEmail(email)
            ?: throw ServiceException.notFound("사용자를 찾을 수 없습니다.")

    @Transactional
    fun updateName(email: String, name: String): User {
        val user = userRepository.findByEmail(email)
            ?: throw ServiceException.notFound("사용자를 찾을 수 없습니다.")

        user.name = name

        user.id?.also { userId ->
            matchCacheService.evictUserProfileCache(userId)
            log.info("유저 이름 변경 - 캐시 무효화 (userId: {})", userId)
        }

        return userRepository.save(user)
    }

    @Transactional
    fun updateEmail(currentEmail: String, request: UserUpdateEmailRequest): User {
        val user = findByEmail(currentEmail)

        val verification = verificationRepository.findByEmail(request.newEmail)
            ?: throw ServiceException.badRequest("인증 요청이 존재하지 않습니다.")

        if (!verification.isVerified) {
            throw ServiceException.badRequest("이메일 인증이 완료되지 않았습니다.")
        }

        if (verification.code != request.code) {
            throw ServiceException.unauthorized("인증 코드가 올바르지 않습니다.")
        }

        if (userRepository.existsByEmail(request.newEmail)) {
            throw ServiceException.badRequest("이미 등록된 이메일입니다.")
        }

        user.email = request.newEmail

        verificationRepository.delete(verification)

        return user
    }
}