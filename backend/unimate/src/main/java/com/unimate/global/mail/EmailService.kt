package com.unimate.global.mail

import org.slf4j.LoggerFactory
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendVerificationEmail(to: String, code: String) {
        try {
            val message = SimpleMailMessage().apply {
                setTo(to)
                subject = "[Unimate] 이메일 인증 코드"
                text = """
                            안녕하세요 😊
        
                            요청하신 이메일 인증 코드는 아래와 같습니다.
        
                            🔐 인증 코드: ${code}
        
                            본 메일은 발신 전용입니다. 10분 내에 인증을 완료해주세요.
                            
                            """.trimIndent()
            }
            mailSender.send(message)



            log.info("[이메일 발송 성공] email={}, code={}", to, code)
        } catch (e: Exception) {
            log.error("[이메일 발송 실패] email={}", to, e)
            throw RuntimeException("이메일 발송 중 오류가 발생했습니다.")
        }
    }
}
