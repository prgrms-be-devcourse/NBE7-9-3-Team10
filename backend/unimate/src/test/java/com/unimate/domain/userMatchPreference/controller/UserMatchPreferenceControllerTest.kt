package com.unimate.domain.userMatchPreference.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.dto.MatchPreferenceRequest
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.domain.userProfile.service.UserProfileService
import com.unimate.global.jwt.JwtProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserMatchPreferenceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var userProfileService: UserProfileService

    @MockitoBean
    private lateinit var matchCacheService: MatchCacheService

    private lateinit var testUser: User
    private lateinit var accessToken: String
    private val baseUrl = "/api/v1/users/me"

    @BeforeEach
    fun setup() {
        userProfileRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            name = "testuser",
            email = "test@test.ac.kr",
            password = passwordEncoder.encode("password"),
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            university = "Test University"
        )
        userRepository.save(testUser)

        val userProfile = UserProfile(
            user = testUser,
            sleepTime = 3,
            isPetAllowed = true,
            isSmoker = false,
            cleaningFrequency = 3,
            preferredAgeGap = 2,
            hygieneLevel = 3,
            isSnoring = false,
            drinkingFrequency = 2,
            noiseSensitivity = 3,
            guestFrequency = 2,
            mbti = "INFP",
            startUseDate = LocalDate.now(),
            endUseDate = LocalDate.now().plusMonths(6),
            matchingEnabled = true
        )
        userProfileRepository.save(userProfile)

        accessToken = jwtProvider.generateToken(testUser.email, testUser.id!!).accessToken
    }

    private fun createSampleRequest(): MatchPreferenceRequest {
        return MatchPreferenceRequest(
            startUseDate = LocalDate.now().plusDays(1),
            endUseDate = LocalDate.now().plusMonths(7),
            sleepTime = 4,
            isPetAllowed = false,
            isSmoker = true,
            cleaningFrequency = 4,
            preferredAgeGap = 3,
            hygieneLevel = 4,
            isSnoring = true,
            drinkingFrequency = 3,
            noiseSensitivity = 4,
            guestFrequency = 3
        )
    }

    @Test
    @DisplayName("매칭 선호도 수정 성공")
    fun `update match preference success`() {
        val request = createSampleRequest()
        val requestJson = objectMapper.writeValueAsString(request)

        mockMvc.perform(
            put("$baseUrl/preferences")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUser.id!!))
            .andExpect(jsonPath("$.sleepTime").value(request.sleepTime))
            .andExpect(jsonPath("$.isSmoker").value(request.isSmoker))
    }

    @Test
    @DisplayName("매칭 선호도 수정 실패 - 유효하지 않은 데이터 (@Max)")
    fun `update match preference fail with invalid data`() {
        val invalidRequest = createSampleRequest().copy(sleepTime = 6) // @Max(5) 위반
        val requestJson = objectMapper.writeValueAsString(invalidRequest)

        mockMvc.perform(
            put("$baseUrl/preferences")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("sleepTime은 5 이하의 값이어야 합니다."))
    }

    @Test
    @DisplayName("매칭 상태 비활성화 성공")
    fun `cancel matching status success`() {
        mockMvc.perform(
            delete("$baseUrl/matching-status")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNoContent)

        verify(userProfileService).cancelMatching(testUser.id!!)
    }

    @Test
    @DisplayName("인증 없이 매칭 선호도 수정 실패")
    fun `update match preference fail without auth`() {
        val request = createSampleRequest()
        val requestJson = objectMapper.writeValueAsString(request)

        mockMvc.perform(
            put("$baseUrl/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isUnauthorized)
    }
}