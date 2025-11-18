package com.unimate.domain.user.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.user.admin.dto.AdminLoginRequest
import com.unimate.domain.user.admin.dto.AdminSignupRequest
import com.unimate.domain.user.admin.repository.AdminRepository
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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminAuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var adminRepository: AdminRepository

    private val baseUrl = "/api/v1/admin/auth"
    private val testEmail = "admin@test.com"
    private val testPassword = "admin1234!"
    private val testName = "관리자"

    @BeforeEach
    fun setup() {
        val signupRequest = AdminSignupRequest(
            email = testEmail,
            password = testPassword,
            name = testName
        )

        mockMvc.perform(
            post("$baseUrl/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(testEmail))
            .andExpect(jsonPath("$.name").value(testName))

        assertThat(adminRepository.findByEmail(testEmail)).isNotNull
    }

    @Test
    @DisplayName("관리자 로그인 성공 - AccessToken과 RefreshToken이 정상 발급된다")
    fun `login success`() {
        val loginRequest = AdminLoginRequest(testEmail, testPassword)

        mockMvc.perform(
            post("$baseUrl/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("adminRefreshToken"))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.adminId").exists())
            .andExpect(jsonPath("$.email").value(testEmail))
    }

    @Test
    @DisplayName("관리자 로그인 실패 - 비밀번호 불일치 시 401 반환")
    fun `login fail wrongPassword`() {
        val loginRequest = AdminLoginRequest(testEmail, "wrongPassword@gmail.com")

        mockMvc.perform(
            post("$baseUrl/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("관리자 RefreshToken으로 AccessToken 재발급 성공")
    fun `refreshToken success`() {
        val loginRequest = AdminLoginRequest(testEmail, testPassword)

        val refreshToken = mockMvc.perform(
            post("$baseUrl/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .getCookie("adminRefreshToken")
            ?.value
            ?: throw AssertionError("RefreshToken이 없습니다.")

        mockMvc.perform(
            post("$baseUrl/token/refresh")
                .cookie(MockCookie("adminRefreshToken", refreshToken))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
    }

    @Test
    @DisplayName("관리자 RefreshToken이 유효하지 않으면 재발급 실패")
    fun `refresh fail invalidToken`() {
        mockMvc.perform(
            post("$baseUrl/token/refresh")
                .cookie(MockCookie("adminRefreshToken", "invalid-token"))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다."))
    }

    @Test
    @DisplayName("관리자 로그아웃 성공 시 RefreshToken 쿠키가 제거된다")
    fun `logout success`() {
        val loginRequest = AdminLoginRequest(testEmail, testPassword)

        val loginResponse = mockMvc.perform(
            post("$baseUrl/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val accessToken = objectMapper.readTree(loginResponse.response.contentAsString)
            .get("accessToken")
            .asText()

        val refreshToken = loginResponse.response.getCookie("adminRefreshToken")?.value
            ?: throw AssertionError("RefreshToken이 없습니다.")

        mockMvc.perform(
            post("$baseUrl/logout")
                .header("Authorization", "Bearer $accessToken")
                .cookie(MockCookie("adminRefreshToken", refreshToken))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().maxAge("adminRefreshToken", 0))
            .andExpect(jsonPath("$.message").value("관리자 로그아웃이 완료되었습니다."))
    }
}