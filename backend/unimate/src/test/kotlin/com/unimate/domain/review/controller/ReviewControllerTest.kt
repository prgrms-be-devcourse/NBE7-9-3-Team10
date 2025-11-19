package com.unimate.domain.review.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.review.dto.ReviewCreateRequest
import com.unimate.domain.review.dto.ReviewUpdateRequest
import com.unimate.domain.review.repository.ReviewRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.mail.EmailService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mail.MailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var passwordEncoder: BCryptPasswordEncoder

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

    @MockitoBean
    private lateinit var mailSender: MailSender

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var reviewer: User
    private lateinit var reviewee: User
    private lateinit var reviewerToken: String
    private lateinit var match: Match

    private val baseUrl = "/api/v1/reviews"

    @BeforeEach
    fun setUp() {
        reviewRepository.deleteAll()
        matchRepository.deleteAll()
        userMatchPreferenceRepository.deleteAll()
        userProfileRepository.deleteAll()
        userRepository.deleteAll()

        reviewer = createUser("reviewer@test.ac.kr", "리뷰어", Gender.MALE)
        reviewee = createUser("reviewee@test.ac.kr", "리뷰이", Gender.FEMALE)

        reviewerToken = loginAndGetToken(reviewer.email, "password123!")

        // 매칭 생성 (90일 이상 경과된 상태로 설정)
        val endDate = LocalDate.now().minusDays(10) // 10일 전 종료
        createUserProfile(reviewer, endDate)
        createUserProfile(reviewee, endDate)

        match = Match(
            sender = reviewer,
            receiver = reviewee,
            matchType = MatchType.LIKE,
            matchStatus = MatchStatus.ACCEPTED,
            confirmedAt = LocalDateTime.now().minusDays(100) // 100일 전 확정
        )
        matchRepository.save(match)
    }

    @Test
    @DisplayName("후기 작성 성공")
    fun createReview_Success() {
        val request = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 5,
            content = "매우 만족합니다!",
            recommend = true
        )

        mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.content").value("매우 만족합니다!"))
            .andExpect(jsonPath("$.recommend").value(true))
            .andExpect(jsonPath("$.reviewerName").value("리뷰어"))
            .andExpect(jsonPath("$.revieweeName").value("리뷰이"))

        val savedReview = reviewRepository.findAll().firstOrNull()
        assertThat(savedReview).isNotNull
        assertThat(savedReview?.rating).isEqualTo(5)
    }

    @Test
    @DisplayName("후기 작성 실패 - 평점 범위 초과")
    fun createReview_Fail_InvalidRating() {
        val request = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 6, // 잘못된 평점
            content = "테스트",
            recommend = true
        )

        mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("후기 작성 실패 - 중복 작성")
    fun createReview_Fail_Duplicate() {
        val request = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 5,
            content = "첫 번째 리뷰",
            recommend = true
        )

        // 첫 번째 리뷰 작성
        mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        // 두 번째 리뷰 작성 시도 (중복)
        mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    @DisplayName("후기 조회 성공")
    fun getReview_Success() {
        // 먼저 리뷰 작성
        val createRequest = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 4,
            content = "좋은 경험이었습니다",
            recommend = true
        )

        val createResponse = mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val reviewId = objectMapper.readTree(createResponse.response.contentAsString).get("reviewId").asLong()

        // 리뷰 조회
        mockMvc.perform(
            get("$baseUrl/$reviewId")
                .header("Authorization", "Bearer $reviewerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reviewId").value(reviewId))
            .andExpect(jsonPath("$.rating").value(4))
            .andExpect(jsonPath("$.content").value("좋은 경험이었습니다"))
    }

    @Test
    @DisplayName("대기 중인 후기 목록 조회 성공")
    fun getPendingReviews_Success() {
        mockMvc.perform(
            get("$baseUrl/pending")
                .header("Authorization", "Bearer $reviewerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].matchId").value(match.id))
            .andExpect(jsonPath("$[0].revieweeName").value("리뷰이"))
            .andExpect(jsonPath("$[0].canCreateReview").value(true))
    }

    @Test
    @DisplayName("후기 수정 성공")
    fun updateReview_Success() {
        // 먼저 리뷰 작성
        val createRequest = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 3,
            content = "초기 리뷰",
            recommend = false
        )

        val createResponse = mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val reviewId = objectMapper.readTree(createResponse.response.contentAsString).get("reviewId").asLong()

        // 리뷰 수정
        val updateRequest = ReviewUpdateRequest(
            rating = 5,
            content = "수정된 리뷰",
            recommend = true
        )

        mockMvc.perform(
            put("$baseUrl/$reviewId")
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.content").value("수정된 리뷰"))
            .andExpect(jsonPath("$.recommend").value(true))
    }

    @Test
    @DisplayName("후기 삭제 성공")
    fun deleteReview_Success() {
        // 먼저 리뷰 작성
        val createRequest = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 4,
            content = "삭제될 리뷰",
            recommend = true
        )

        val createResponse = mockMvc.perform(
            post(baseUrl)
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val reviewId = objectMapper.readTree(createResponse.response.contentAsString).get("reviewId").asLong()

        // 리뷰 삭제
        mockMvc.perform(
            delete("$baseUrl/$reviewId")
                .header("Authorization", "Bearer $reviewerToken")
        )
            .andExpect(status().isNoContent)

        // 삭제 확인
        val deletedReview = reviewRepository.findById(reviewId)
        assertThat(deletedReview).isEmpty
    }

    @Test
    @DisplayName("재매칭 가능 여부 확인")
    fun canRematch_Success() {
        mockMvc.perform(
            get("$baseUrl/match/${match.id}/can-rematch")
                .header("Authorization", "Bearer $reviewerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.canRematch").exists())
    }

    // Helper methods
    private fun createUser(email: String, name: String, gender: Gender): User {
        val user = User(
            email = email,
            password = passwordEncoder.encode("password123!"),
            name = name,
            gender = gender,
            birthDate = LocalDate.of(2000, 1, 1),
            university = "테스트대학교"
        )
        return userRepository.save(user)
    }

    private fun createUserProfile(user: User, endDate: LocalDate) {
        val profile = UserProfile(
            user = user,
            sleepTime = 3, // 00시~02시
            isPetAllowed = false,
            isSmoker = false,
            cleaningFrequency = 3, // 주 1회
            preferredAgeGap = 5,
            hygieneLevel = 3,
            isSnoring = false,
            drinkingFrequency = 2,
            noiseSensitivity = 3,
            guestFrequency = 3,
            mbti = "ISTJ",
            startUseDate = endDate.minusDays(180),
            endUseDate = endDate,
            matchingEnabled = true
        )
        userProfileRepository.save(profile)
    }

    private fun loginAndGetToken(email: String, password: String): String {
        val loginRequest = com.unimate.domain.user.user.dto.UserLoginRequest(email, password)
        val response = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseBody = objectMapper.readTree(response.response.contentAsString)
        return responseBody.get("accessToken").asText()
    }
}
