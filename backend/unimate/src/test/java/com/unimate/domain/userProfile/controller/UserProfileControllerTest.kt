package com.unimate.domain.userProfile.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userProfile.dto.ProfileCreateRequest
import com.unimate.domain.userProfile.repository.UserProfileRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserProfileControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userRepository: UserRepository,
    @Autowired val userProfileRepository: UserProfileRepository,
    @Autowired val passwordEncoder: BCryptPasswordEncoder
) {
    //---------------------- FIXTURE ------------------------------
    companion object {
        private const val TEST_EMAIL = "tester@test.ac.kr"
        private const val TEST_PASSWORD = "test1234"
        private const val TEST_NAME = "테스트유저"
        private const val TEST_UNIVERSITY = "서울대"
    }

    @BeforeEach
    fun setUp() {
        // 이미 존재하면 중복 생성 방지
        userRepository.findByEmail(TEST_EMAIL)?.let { userRepository.delete(it) }

        val user = User(
            TEST_NAME,
            TEST_EMAIL,
            passwordEncoder.encode(TEST_PASSWORD),
            Gender.MALE,
            LocalDate.of(1991, 1, 1),
            TEST_UNIVERSITY
        ).apply { studentVerified = true }

        userRepository.save(user)

        // 프로필 테이블은 비워두고 시작
        userProfileRepository.deleteAll()
    }

    //---------------------- HELPER -------------------------------
    private fun loginAndGetAccessToken(): String {
        val body = """
            {
                "email":"$TEST_EMAIL",
                "password":"$TEST_PASSWORD"
            }
        """.trimIndent()

        val json = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isString)
            .andReturn()
            .response
            .contentAsString

        val token = objectMapper.readTree(json).path("accessToken").asText(null)
        assertThat(token)
            .`as`("AccessToken should not be null or blank")
            .isNotBlank
        return requireNotNull(token) { "AccessToken이 null입니다." }
    }

    private fun String.bearer() = "Bearer $this"

    private fun sampleCreateReq() = ProfileCreateRequest(
        sleepTime = 1,
        isPetAllowed = true,
        isSmoker = false,
        cleaningFrequency = 2,
        preferredAgeGap = 3,
        hygieneLevel = 3,
        isSnoring = true,
        drinkingFrequency = 2,
        noiseSensitivity = 3,
        guestFrequency = 1,
        mbti = "INFP",
        startUseDate = LocalDate.now(),
        endUseDate = LocalDate.now().plusMonths(6),
        matchingEnabled = false
    )

    private fun sampleUpdateReq() = ProfileCreateRequest(
        sleepTime = 2,
        isPetAllowed = false,
        isSmoker = false,
        cleaningFrequency = 3,
        preferredAgeGap = 5,
        hygieneLevel = 1,
        isSnoring = false,
        drinkingFrequency = 3,
        noiseSensitivity = 4,
        guestFrequency = 2,
        mbti = "ENTP",
        startUseDate = LocalDate.now(),
        endUseDate = LocalDate.now().plusMonths(6),
        matchingEnabled = true
    )

    //---------------------- TEST CODE ----------------------------
    @Test
    @DisplayName("프로필 생성 성공 테스트")
    fun `프로필 생성 성공`() {
        // given
        val token = loginAndGetAccessToken()
        val request = sampleCreateReq()
        val requestJson = objectMapper.writeValueAsString(request)

        // when
        val responseJson = mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.sleepTime").value(request.sleepTime))
            .andExpect(jsonPath("$.isPetAllowed").value(request.isPetAllowed))
            .andExpect(jsonPath("$.mbti").value(request.mbti))
            .andReturn()
            .response
            .contentAsString

        // then
        val responseNode = objectMapper.readTree(responseJson)
        val profileId = responseNode.get("id").asLong()

        assertThat(profileId).isPositive()
        assertThat(userProfileRepository.findById(profileId))
            .isPresent
            .hasValueSatisfying { profile ->
                assertThat(profile.sleepTime).isEqualTo(request.sleepTime)
                assertThat(profile.isPetAllowed).isEqualTo(request.isPetAllowed)
                assertThat(profile.mbti).isEqualTo(request.mbti)
            }
    }

    @Test
    @DisplayName("프로필 조회 성공 테스트")
    fun `프로필 조회 성공`() {
        // given
        val token = loginAndGetAccessToken()
        val request = sampleCreateReq()

        // 프로필 생성
        val createJson = objectMapper.writeValueAsString(request)
        val createdResponse = mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val profileId = objectMapper.readTree(createdResponse).get("id").asLong()

        // when & then
        mockMvc.perform(
            get("/api/v1/profile")
                .header("Authorization", token.bearer())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(profileId))
            .andExpect(jsonPath("$.sleepTime").value(request.sleepTime))
            .andExpect(jsonPath("$.isPetAllowed").value(request.isPetAllowed))
            .andExpect(jsonPath("$.mbti").value(request.mbti))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @DisplayName("프로필 수정 성공 테스트")
    fun `프로필 수정 성공`() {
        // given
        val token = loginAndGetAccessToken()

        // 프로필 생성
        val createJson = objectMapper.writeValueAsString(sampleCreateReq())
        val createdResponse = mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val profileId = objectMapper.readTree(createdResponse).get("id").asLong()

        // 프로필 수정
        val updateRequest = sampleUpdateReq()
        val updateJson = objectMapper.writeValueAsString(updateRequest)

        // when
        val updatedResponse = mockMvc.perform(
            put("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(profileId))
            .andExpect(jsonPath("$.sleepTime").value(updateRequest.sleepTime))
            .andExpect(jsonPath("$.isPetAllowed").value(updateRequest.isPetAllowed))
            .andExpect(jsonPath("$.hygieneLevel").value(updateRequest.hygieneLevel))
            .andExpect(jsonPath("$.mbti").value(updateRequest.mbti))
            .andExpect(jsonPath("$.matchingEnabled").value(updateRequest.matchingEnabled))
            .andReturn()
            .response
            .contentAsString

        // then
        val responseNode = objectMapper.readTree(updatedResponse)
        assertThat(responseNode.get("sleepTime").asInt()).isEqualTo(updateRequest.sleepTime)
        assertThat(responseNode.get("isPetAllowed").asBoolean()).isEqualTo(updateRequest.isPetAllowed)
        assertThat(responseNode.get("hygieneLevel").asInt()).isEqualTo(updateRequest.hygieneLevel)
        assertThat(responseNode.get("mbti").asText()).isEqualTo(updateRequest.mbti)

        // DB에서도 확인
        val updatedProfile = userProfileRepository.findById(profileId).orElseThrow()
        assertThat(updatedProfile.sleepTime).isEqualTo(updateRequest.sleepTime)
        assertThat(updatedProfile.isPetAllowed).isEqualTo(updateRequest.isPetAllowed)
        assertThat(updatedProfile.hygieneLevel).isEqualTo(updateRequest.hygieneLevel)
        assertThat(updatedProfile.mbti).isEqualTo(updateRequest.mbti)
    }

    @Test
    @DisplayName("인증 없이 프로필 생성 시 401 반환")
    fun `인증 없이 프로필 생성 시 실패`() {
        // given
        val request = sampleCreateReq()
        val requestJson = objectMapper.writeValueAsString(request)

        // when & then
        mockMvc.perform(
            post("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("잘못된 토큰으로 프로필 조회 시 401 반환")
    fun `잘못된 토큰으로 프로필 조회 시 실패`() {
        // when & then
        mockMvc.perform(
            get("/api/v1/profile")
                .header("Authorization", "Bearer invalid-token")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("프로필이 없는 상태에서 조회 시 404 반환")
    fun `프로필이 없는 상태에서 조회 시 실패`() {
        // given
        val token = loginAndGetAccessToken()

        // when & then
        mockMvc.perform(
            get("/api/v1/profile")
                .header("Authorization", token.bearer())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("프로필이 없는 상태에서 수정 시 실패")
    fun `프로필이 없는 상태에서 수정 시 실패`() {
        // given
        val token = loginAndGetAccessToken()
        val updateJson = objectMapper.writeValueAsString(sampleUpdateReq())

        // when & then
        mockMvc.perform(
            put("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("이미 프로필이 있는 상태에서 중복 생성 시 실패")
    fun `프로필 중복 생성 시 실패`() {
        // given
        val token = loginAndGetAccessToken()
        val requestJson = objectMapper.writeValueAsString(sampleCreateReq())

        // 첫 번째 프로필 생성
        mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)

        // when & then - 두 번째 프로필 생성 시도
        mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isConflict)
    }
}

