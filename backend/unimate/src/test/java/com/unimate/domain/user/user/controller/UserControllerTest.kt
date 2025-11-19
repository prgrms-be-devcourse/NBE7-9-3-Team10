package com.unimate.domain.user.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.user.user.dto.UserUpdateEmailRequest
import com.unimate.domain.user.user.dto.UserUpdateNameRequest
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.verification.entity.Verification
import com.unimate.domain.verification.repository.VerificationRepository
import com.unimate.global.jwt.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var verificationRepository: VerificationRepository

    @Autowired
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    private lateinit var accessToken: String
    private val baseUrl = "/api/v1/user"
    private val testEmail = "testuser@university.ac.kr"

    @BeforeEach
    fun setup() {
        val user = User(
            name = "홍길동",
            email = testEmail,
            password = passwordEncoder.encode("password123!"),
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            university = "고려대학교"
        )
        val savedUser = userRepository.save(user)

        val userId = requireNotNull(savedUser.id) { "저장된 사용자의 ID가 null입니다." }

        accessToken = jwtProvider.generateToken(testEmail, userId).accessToken
    }

    @Test
    @DisplayName("GET /api/v1/user - 사용자 정보 조회 성공")
    fun `get user info success`() {
        mockMvc.perform(
            get(baseUrl)
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(testEmail))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.university").value("고려대학교"))
            .andExpect(jsonPath("$.gender").value("MALE"))
    }

    @Test
    @DisplayName("PATCH /api/v1/user/name - 사용자 이름 수정 성공")
    fun `update user name success`() {
        val request = UserUpdateNameRequest("새로운이름")

        mockMvc.perform(
            patch("$baseUrl/name")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("새로운이름"))

        val updated = userRepository.findByEmail(testEmail).orElseThrow()
        assertThat(updated.name).isEqualTo("새로운이름")
    }

    @Test
    @DisplayName("PATCH /api/v1/user/email - 이메일 수정 성공 (인증 완료된 경우)")
    fun `update user email success`() {
        val emailPrefix = "newtest"
        val code = "123456"

        val fullEmail = "$emailPrefix@biomedical.korea.ac.kr"
        val verification = Verification(fullEmail, code, LocalDateTime.now().plusMinutes(5))
        verification.markVerified()
        verificationRepository.save(verification)

        val request = UserUpdateEmailRequest(emailPrefix, code)
        mockMvc.perform(
            patch("$baseUrl/email")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(fullEmail))  // ← 완전한 이메일로 검증

        val updated = userRepository.findByEmail(fullEmail).orElseThrow()
        assertThat(updated.email).isEqualTo(fullEmail)
    }

    @Test
    @DisplayName("PATCH /api/v1/user/email - 인증되지 않은 이메일로 수정 시 실패")
    fun `update user email fail unverified`() {
        val emailPrefix = "failtest"
        val fullEmail = "$emailPrefix@biomedical.korea.ac.kr"

        val verification = Verification(fullEmail, "000000", LocalDateTime.now().plusMinutes(5))
        verificationRepository.save(verification)

        val request = UserUpdateEmailRequest(emailPrefix, "000000")

        mockMvc.perform(
            patch("$baseUrl/email")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다."))
    }
}