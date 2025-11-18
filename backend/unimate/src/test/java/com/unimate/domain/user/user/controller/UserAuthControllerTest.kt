package com.unimate.domain.user.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.user.user.dto.UserLoginRequest
import com.unimate.domain.user.user.dto.UserSignupRequest
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.verification.repository.VerificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockCookie
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserAuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var verificationRepository: VerificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val baseUrl = "/api/v1"
    private val testEmail = "test@university.ac.kr"
    private val testPassword = "password123!"

    @BeforeEach
    fun setup() {
        // 이메일 인증 요청
        mockMvc.perform(
            post("$baseUrl/email/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$testEmail"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("인증코드가 발송되었습니다."))

        // 인증 코드 확인
        val verification = verificationRepository.findByEmail(testEmail)
            ?: throw IllegalStateException("인증 요청이 저장되지 않았습니다.")

        mockMvc.perform(
            post("$baseUrl/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$testEmail", "code":"${verification.code}"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))

        assertThat(verification.isVerified).isTrue

        // 회원가입
        val signupRequest = UserSignupRequest(
            email = testEmail,
            password = testPassword,
            name = "테스트유저",
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            university = "Test University"
        )

        mockMvc.perform(
            post("$baseUrl/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(testEmail))

        assertThat(userRepository.findByEmail(testEmail)).isPresent
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 없이 시도 시 400 반환")
    fun `signup fail without verification`() {
        val request = UserSignupRequest(
            email = "unverified@university.ac.kr",
            password = "password123!",
            name = "미인증유저",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1999, 3, 3),
            university = "Test University"
        )

        mockMvc.perform(
            post("$baseUrl/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("이메일 인증이 필요합니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 가입된 이메일일 경우 400 반환")
    fun `signup fail duplicate email`() {
        val request = UserSignupRequest(
            email = testEmail,
            password = testPassword,
            name = "홍길동",
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 5, 5),
            university = "Test University"
        )

        mockMvc.perform(
            post("$baseUrl/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."))
    }

    @Test
    @DisplayName("로그인 성공 시 AccessToken과 RefreshToken이 정상 발급된다")
    fun `login success`() {
        val loginRequest = UserLoginRequest(testEmail, testPassword)

        mockMvc.perform(
            post("$baseUrl/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("refreshToken"))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.userId").exists())
            .andExpect(jsonPath("$.email").value(testEmail))
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 401 반환")
    fun `login fail wrong password`() {
        val loginRequest = UserLoginRequest(testEmail, "wrongPassword")

        mockMvc.perform(
            post("$baseUrl/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("RefreshToken으로 AccessToken 재발급 성공")
    fun `refresh token success`() {
        val loginRequest = UserLoginRequest(testEmail, testPassword)

        val refreshToken = mockMvc.perform(
            post("$baseUrl/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .getCookie("refreshToken")!!
            .value

        mockMvc.perform(
            post("$baseUrl/auth/token/refresh")
                .cookie(MockCookie("refreshToken", refreshToken))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
    }

    @Test
    @DisplayName("RefreshToken이 유효하지 않으면 재발급 실패")
    fun `refresh fail invalid token`() {
        mockMvc.perform(
            post("$baseUrl/auth/token/refresh")
                .cookie(MockCookie("refreshToken", "invalid-token"))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다."))
    }

    @Test
    @DisplayName("로그아웃 성공 시 RefreshToken 쿠키가 제거된다")
    fun `logout success`() {
        val loginRequest = UserLoginRequest(testEmail, testPassword)

        val loginResult = mockMvc.perform(
            post("$baseUrl/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val accessToken = objectMapper.readTree(loginResult.response.contentAsString)
            .get("accessToken")
            .asText()

        val refreshToken = loginResult.response.getCookie("refreshToken")!!.value

        mockMvc.perform(
            post("$baseUrl/auth/logout")
                .header("Authorization", "Bearer $accessToken")
                .cookie(MockCookie("refreshToken", refreshToken))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().maxAge("refreshToken", 0))
            .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
    }
}