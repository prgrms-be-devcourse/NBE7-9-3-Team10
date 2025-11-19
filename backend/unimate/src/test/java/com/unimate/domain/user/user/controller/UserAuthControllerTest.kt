package com.unimate.domain.user.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.user.user.dto.UserLoginRequest
import com.unimate.domain.user.user.dto.UserSignupRequest
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.verification.dto.EmailCodeVerifyRequest
import com.unimate.domain.verification.dto.EmailVerificationRequest
import com.unimate.domain.verification.repository.VerificationRepository
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.review.repository.ReviewRepository
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.repository.UserProfileRepository
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

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var userMatchPreferenceRepository: UserMatchPreferenceRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    private val baseUrl = "/api/v1"
    private val testEmail = "testuser@snu.ac.kr"
    private val testPassword = "password123!"
    private val testUniversity = "서울대학교"

    @BeforeEach
    fun setup() {
        reviewRepository.deleteAll()
        matchRepository.deleteAll()
        userMatchPreferenceRepository.deleteAll()
        userProfileRepository.deleteAll()
        userRepository.deleteAll()
        verificationRepository.deleteAll()

        // 1단계: 이메일 인증 요청
        val verificationRequest = EmailVerificationRequest(testEmail)

        mockMvc.perform(
            post("$baseUrl/email/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("인증코드가 발송되었습니다."))

        // 2단계: 인증 코드 확인
        val verification = verificationRepository.findByEmail(testEmail)
            ?: throw IllegalStateException("인증 요청이 저장되지 않았습니다.")

        val verifyRequest = EmailCodeVerifyRequest(
            email = testEmail,
            code = verification.code
        )

        mockMvc.perform(
            post("$baseUrl/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))

        // 인증 완료 확인
        val verifiedVerification = verificationRepository.findByEmail(testEmail)
        assertThat(verifiedVerification).isNotNull
        assertThat(verifiedVerification?.isVerified).isTrue()

        // 3단계: 회원가입
        val signupRequest = UserSignupRequest(
            email = testEmail,
            password = testPassword,
            name = "테스트유저",
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            university = testUniversity
        )

        mockMvc.perform(
            post("$baseUrl/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(testEmail))

        // 회원가입 완료 확인
        assertThat(userRepository.findByEmail(testEmail))
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 없이 시도 시 400 반환")
    fun `signup fail without verification`() {
        val unverifiedEmail = "unverified@yonsei.ac.kr"

        val request = UserSignupRequest(
            email = unverifiedEmail,
            password = "password123!",
            name = "미인증유저",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1999, 3, 3),
            university = "연세대학교"
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
    @DisplayName("회원가입 실패 - 이미 가입된 이메일일 경우 409 반환")
    fun `signup fail duplicate email`() {
        val request = UserSignupRequest(
            email = testEmail,
            password = testPassword,
            name = "홍길동",
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 5, 5),
            university = testUniversity
        )

        mockMvc.perform(
            post("$baseUrl/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
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
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자일 경우 401 반환")
    fun `login fail user not found`() {
        val loginRequest = UserLoginRequest("notexist@snu.ac.kr", testPassword)

        mockMvc.perform(
            post("$baseUrl/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("이메일이 일치하지 않습니다."))
    }

    @Test
    @DisplayName("RefreshToken으로 AccessToken 재발급 성공")
    fun `refresh token success`() {
        val loginRequest = UserLoginRequest(testEmail, testPassword)

        val loginResponse = mockMvc.perform(
            post("$baseUrl/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val refreshToken = loginResponse.response.getCookie("refreshToken")?.value
            ?: throw IllegalStateException("RefreshToken이 없습니다.")

        val accessToken = objectMapper.readTree(loginResponse.response.contentAsString)
            .get("accessToken")
            .asText()

        mockMvc.perform(
            post("$baseUrl/auth/token/refresh")
                .header("Authorization", "Bearer $accessToken")
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
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("RefreshToken 없이 요청하면 재발급 실패 - 400 반환")
    fun `refresh fail no token`() {
        mockMvc.perform(
            post("$baseUrl/auth/token/refresh")
        )
            .andExpect(status().isBadRequest)
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

        val refreshToken = loginResult.response.getCookie("refreshToken")?.value
            ?: throw IllegalStateException("RefreshToken이 없습니다.")

        mockMvc.perform(
            post("$baseUrl/auth/logout")
                .header("Authorization", "Bearer $accessToken")
                .cookie(MockCookie("refreshToken", refreshToken))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().maxAge("refreshToken", 0))
            .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
    }

    @Test
    @DisplayName("로그아웃 실패 - Authorization 헤더가 없으면 401 반환")
    fun `logout fail no token`() {
        mockMvc.perform(
            post("$baseUrl/auth/logout")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("이메일 인증 요청 - 정상 요청")
    fun `email verification request success`() {
        val email = "newuser@korea.ac.kr"
        val request = EmailVerificationRequest(email)

        mockMvc.perform(
            post("$baseUrl/email/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("인증코드가 발송되었습니다."))

        // 인증 레코드가 생성되었는지 확인
        assertThat(verificationRepository.findByEmail(email)).isNotNull()
    }

    @Test
    @DisplayName("이메일 인증 요청 실패 - 잘못된 이메일 형식")
    fun `email verification request fail invalid email`() {
        val request = EmailVerificationRequest("invalid-email")

        mockMvc.perform(
            post("$baseUrl/email/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("이메일 인증 - 정상 인증")
    fun `email verification success`() {
        val email = "verify@yonsei.ac.kr"
        val verificationRequest = EmailVerificationRequest(email)

        // 인증 요청
        mockMvc.perform(
            post("$baseUrl/email/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest))
        )
            .andExpect(status().isOk)

        // 인증 코드 조회
        val verification = verificationRepository.findByEmail(email)
            ?: throw IllegalStateException("인증이 저장되지 않았습니다.")

        // 인증 코드 검증
        val verifyRequest = EmailCodeVerifyRequest(
            email = email,
            code = verification.code
        )

        mockMvc.perform(
            post("$baseUrl/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))
    }

    @Test
    @DisplayName("이메일 인증 실패 - 잘못된 코드")
    fun `email verification fail wrong code`() {
        val email = "wrongcode@korea.ac.kr"
        val verificationRequest = EmailVerificationRequest(email)

        // 인증 요청
        mockMvc.perform(
            post("$baseUrl/email/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest))
        )
            .andExpect(status().isOk)

        // 잘못된 코드로 검증
        val verifyRequest = EmailCodeVerifyRequest(
            email = email,
            code = "000000"
        )

        mockMvc.perform(
            post("$baseUrl/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("인증코드가 올바르지 않습니다."))
    }

    @Test
    @DisplayName("이메일 인증 실패 - 인증 요청이 없음")
    fun `email verification fail no request`() {
        val verifyRequest = EmailCodeVerifyRequest(
            email = "norequest@snu.ac.kr",
            code = "123456"
        )

        mockMvc.perform(
            post("$baseUrl/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("인증 요청 기록이 없습니다."))
    }
}