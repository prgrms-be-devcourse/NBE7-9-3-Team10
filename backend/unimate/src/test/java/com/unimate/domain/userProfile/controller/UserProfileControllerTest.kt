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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
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
    val email = "tester@test.ac.kr"
    val rawPassword = "test1234"

    @BeforeEach
    fun setUp() {
        // 이미 존재하면 중복 생성 방지
        userRepository.findByEmail(email).ifPresent { userRepository.delete(it) }

        val user = User(
            "테스트유저",
            email,
            passwordEncoder.encode(rawPassword),
            Gender.MALE,
            LocalDate.of(1991, 1, 1),
            "서울대"
        ).apply { verifyStudent() }

        userRepository.save(user)

        // 프로필 테이블은 비워두고 시작
        userProfileRepository.deleteAll()
    }

    //---------------------- HELPER -------------------------------
    fun loginAndGetAccessToken(): String {
        val body = """
            {
                "email":"$email",
                "password":"$rawPassword"
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
            .`as`("login accessToken null")
            .isNotBlank
        return requireNotNull(token) { "AccessToken이 null입니다." }
    }

    fun String.bearer() = "Bearer $this"

    fun sampleCreateReq() = ProfileCreateRequest(
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
        endUseDate = LocalDate.now(),
        matchingEnabled = false
    )

    fun sampleUpdateReq() = ProfileCreateRequest(
        sleepTime = 1,
        isPetAllowed = false,
        isSmoker = false,
        cleaningFrequency = 2,
        preferredAgeGap = 3,
        hygieneLevel = 1,
        isSnoring = false,
        drinkingFrequency = 2,
        noiseSensitivity = 4,
        guestFrequency = 1,
        mbti = "ENTP",
        startUseDate = LocalDate.now(),
        endUseDate = LocalDate.now(),
        matchingEnabled = false
    )

    //---------------------- TEST CODE ----------------------------
    @Test
    @DisplayName("프로필 생성 성공 테스트")
    fun `프로필 생성 성공`() {
        val token = loginAndGetAccessToken()

        val reqJson = objectMapper.writeValueAsString(sampleCreateReq())
        val resJson = mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqJson)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()
            .response
            .contentAsString

        val node = objectMapper.readTree(resJson)
        val profileId = node.get("id").asLong()

        // 검증은 id만 확인해보기
        assertThat(profileId).isPositive()
        assertThat(userProfileRepository.findById(profileId)).isPresent
    }

    @Test
    @DisplayName("프로필 수정 성공 테스트")
    fun `프로필 수정 성공`() {
        val token = loginAndGetAccessToken()

        // 생성
        val createJson = objectMapper.writeValueAsString(sampleCreateReq())
        val created = mockMvc.perform(
            post("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val id = objectMapper.readTree(created).get("id").asLong()

        // 수정
        val updateJson = objectMapper.writeValueAsString(sampleUpdateReq())

        val updated = mockMvc.perform(
            put("/api/v1/profile")
                .header("Authorization", token.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andReturn()
            .response
            .contentAsString

        // 검증
        val node = objectMapper.readTree(updated)
        assertThat(node.get("sleepTime").asInt()).isEqualTo(1)
        assertThat(node.get("isPetAllowed").asBoolean()).isEqualTo(false)
        assertThat(node.get("mbti").asText()).isEqualTo("ENTP")
    }
}

