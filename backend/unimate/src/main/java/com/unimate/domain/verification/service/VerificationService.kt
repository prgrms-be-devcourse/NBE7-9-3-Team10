package com.unimate.domain.verification.service

import com.unimate.domain.verification.entity.Verification
import com.unimate.domain.verification.repository.VerificationRepository
import com.unimate.global.exception.ServiceException
import com.unimate.global.mail.EmailService
import com.unimate.global.util.VerificationCodeGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class VerificationService(
    private val verificationRepository: VerificationRepository,
    private val emailService: EmailService,
    private val codeGenerator: VerificationCodeGenerator
) {

    private val log = LoggerFactory.getLogger(VerificationService::class.java)

    @Transactional
    fun sendVerificationCode(email: String) {
        val code = codeGenerator.generate6Digits()
        val expiresAt = LocalDateTime.now().plusMinutes(10)

        val existingVerification = verificationRepository.findByEmail(email)

        if (existingVerification != null) {
            existingVerification.updateCode(code, expiresAt)
        } else {
            verificationRepository.save(Verification(email, code, expiresAt))
        }

        log.info("[인증코드 발송 완료] email={}, code={}", email, code)
    }

    @Transactional
    fun verifyCode(email: String, code: String) {
        val v = verificationRepository.findByEmail(email)
            ?: throw ServiceException.notFound("인증 요청 기록이 없습니다.")

        if (v.isExpired) {
            throw ServiceException.badRequest("인증코드가 만료되었습니다.")
        }

        if (v.code != code) {
            throw ServiceException.badRequest("인증코드가 올바르지 않습니다.")
        }

        v.markVerified()
        log.info("[이메일 인증 성공] email={}", email)
    }

    fun assertVerifiedEmailOrThrow(email: String) {
        val v = verificationRepository.findByEmail(email)
            ?: throw ServiceException.badRequest("이메일 인증이 필요합니다.")

        if (!v.isVerified) {
            throw ServiceException.badRequest("이메일 인증이 완료되지 않았습니다.")
        }

        if (v.isExpired) {
            throw ServiceException.badRequest("인증코드가 만료되었습니다. 다시 요청해주세요.")
        }
    }

    @Transactional
    fun consumeVerification(email: String) {
        verificationRepository.deleteByEmail(email)
    }
}