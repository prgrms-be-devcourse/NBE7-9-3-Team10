package com.unimate.domain.user.user.service

import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.dto.UserUpdateEmailRequest
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.verification.repository.VerificationRepository
import com.unimate.global.exception.ServiceException
import com.unimate.global.school.service.SchoolService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val verificationRepository: VerificationRepository,
    private val matchCacheService: MatchCacheService,
    private val schoolService: SchoolService
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun findByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { ServiceException.notFound("사용자를 찾을 수 없습니다.") }
    }

    @Transactional
    fun updateName(email: String, name: String): User {
        val user = userRepository.findByEmail(email)
            .orElseThrow { ServiceException.notFound("사용자를 찾을 수 없습니다.") }
        user.updateName(name)

        user.id?.let { userId ->
            matchCacheService.evictUserProfileCache(userId)
            log.info("유저 이름 변경 - 캐시 무효화 (userId: {})", userId)
        }

        return userRepository.save(user)
    }

    @Transactional
    fun updateEmail(currentEmail: String, request: UserUpdateEmailRequest): User {
        val user = findByEmail(currentEmail)

        // SchoolService를 사용하여 학교 도메인 조회
        val domain = schoolService.getPrimarySchoolDomain(user.university)
            ?: throw ServiceException.badRequest(
                "${user.university}의 도메인을 찾을 수 없습니다."
            )

        // emailPrefix + domain으로 완전한 이메일 생성
        val newEmail = "${request.emailPrefix}@$domain"

        // 이메일 검증 기록 확인
        val verification = verificationRepository.findByEmail(newEmail)
            ?: throw ServiceException.badRequest("인증 요청이 존재하지 않습니다.")

        if (!verification.isVerified) {
            throw ServiceException.badRequest("이메일 인증이 완료되지 않았습니다.")
        }

        if (verification.code != request.code) {
            throw ServiceException.unauthorized("인증 코드가 올바르지 않습니다.")
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw ServiceException.badRequest("이미 등록된 이메일입니다.")
        }

        user.updateEmail(newEmail)

        verificationRepository.delete(verification)

        log.info("사용자 이메일 변경: {} -> {}", currentEmail, newEmail)

        return user
    }

    /**
     * 대학교의 이메일 도메인 조회
     * schools.json에서 학교명을 비교하여 도메인 조회
     */
    fun getUniversityDomain(university: String): String? {
        return schoolService.getPrimarySchoolDomain(university)
    }
}