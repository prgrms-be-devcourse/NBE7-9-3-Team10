package com.unimate.domain.match.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.match.dto.LikeRequest
import com.unimate.domain.match.dto.MatchConfirmRequest
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.dto.UserLoginRequest
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
 class MatchControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var passwordEncoder: BCryptPasswordEncoder
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var userProfileRepository: UserProfileRepository
    @Autowired private lateinit var userMatchPreferenceRepository: UserMatchPreferenceRepository
    @Autowired private lateinit var matchRepository: MatchRepository
    @Autowired private lateinit var matchCacheService: MatchCacheService

    @MockitoBean private lateinit var mailSender: MailSender
    @MockitoBean private lateinit var emailService: EmailService

    private lateinit var sender: User
    private lateinit var receiver: User
    private lateinit var thirdUser: User
    private lateinit var senderToken: String
    private lateinit var receiverToken: String

    private val baseUrl = "/api/v1/matches"

    @BeforeEach
    fun setUp() {
        matchRepository.deleteAll()
        userMatchPreferenceRepository.deleteAll()
        userProfileRepository.deleteAll()
        userRepository.deleteAll()

        sender = createUser("sender@test.ac.kr", "송신자", Gender.MALE)
        receiver = createUser("receiver@test.ac.kr", "수신자", Gender.MALE)
        // thirdUser는 동일 대학으로 설정하여 추천 목록에 포함되도록 함
        thirdUser = createUser("third@test.ac.kr", "제삼자", Gender.MALE)

        createUserProfile(sender, true)
        createUserProfile(receiver, true)
        createUserProfile(thirdUser, true)

        createUserPreference(sender)
        createUserPreference(receiver)
        createUserPreference(thirdUser)

        // 캐시 무효화 후 재로딩 (테스트 데이터가 캐시에 반영되도록)
        // 개별 프로필 캐시도 무효화 (getUserProfileById에서 사용)
        evictUserCache(sender)
        evictUserCache(receiver)
        evictUserCache(thirdUser)
        // 전체 후보 캐시도 무효화
        matchCacheService.evictAllCandidatesCache()


        senderToken = login(sender.email, "password123!")
        receiverToken = login(receiver.email, "password123!")
    }

    private fun createUser(email: String, name: String, gender: Gender): User {
        val u = User(
            name,
            email,
            passwordEncoder.encode("password123!"),
            gender,
            LocalDate.now().minusYears(27),  // 26-28세 범위에 맞추기 위해 27세로 설정
            "서울대학교"
        )
        u.studentVerified = true
        return userRepository.save(u)
    }

    private fun createUserProfile(user: User, enabled: Boolean) {
        val profile = UserProfile(
            user,
            3,
            true,
            false,
            3,
            2,
            3,
            false,
            1,
            2,
            1,
            "INTP",
            LocalDate.now(),
            LocalDate.now(),
            enabled
        )
        userProfileRepository.save(profile)
    }

    private fun createUserPreference(user: User) {
        val pref = UserMatchPreference(
            user,
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            3,  // sleepTime
            true,  // isPetAllowed
            false,  // isSmoker
            3,  // cleaningFrequency
            2,  // preferredAgeGap
            3,  // hygieneLevel
            false,  // isSnoring
            1,  // drinkingFrequency
            2,  // noiseSensitivity
            1 // guestFrequency
        )
        userMatchPreferenceRepository.save(pref)
    }

    private fun login(email: String, password: String): String {
        val req = UserLoginRequest(email, password)

        val json = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(json).path("accessToken").asText()
    }

    private fun bearer(token: String): String = "Bearer $token"

    private fun User.getIdOrThrow(): Long = id ?: error("User ID가 null입니다. 저장 후 ID가 생성되어야 합니다.")

    private fun Match.getIdOrThrow(): Long = id ?: error("Match ID가 null입니다. 저장 후 ID가 생성되어야 합니다.")

    private fun evictUserCache(user: User) {
        user.id?.let { matchCacheService.evictUserProfileCache(it) }
    }

    // ───────────────────────────────────────────────
    @Test
    @DisplayName("추천 목록 조회 성공")
    fun getRecommendations_success() {
        // receiver와 thirdUser가 추천 목록에 포함되어야 함
        // (sender 자신은 제외, 동일 성별, 동일 대학, 필터 조건 만족)
        mockMvc.perform(
            get("$baseUrl/recommendations")
                .header("Authorization", bearer(senderToken))
                .param("sleepPattern", "normal")
                .param("ageRange", "26-28") // 23-25 → 26-28로 변경
                .param("cleaningFrequency", "weekly")
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().plusMonths(6).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.recommendations").isArray())
            .andExpect(jsonPath("$.recommendations.length()").value(2)) // receiver, thirdUser
    }

    @Test
    @DisplayName("추천 상세 조회 성공 - Match가 있는 경우")
    fun getCandidateDetail_success_withMatch() {
        // Match 생성
        val existingMatch = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.8))
        matchRepository.save(existingMatch)

        val receiverId = receiver.getIdOrThrow()

        mockMvc.perform(
            get("$baseUrl/candidates/$receiverId")
                .header("Authorization", bearer(senderToken))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.receiverId").value(receiverId))
            .andExpect(jsonPath("$.matchType").value("REQUEST"))
            .andExpect(jsonPath("$.matchStatus").value("PENDING"))
            .andExpect(jsonPath("$.preferenceScore").exists())
            .andExpect(jsonPath("$.name").value(receiver.name))
            .andExpect(jsonPath("$.email").value(receiver.email))
            .andExpect(jsonPath("$.university").value(receiver.university))
    }

    @Test
    @DisplayName("좋아요 보내기 성공")
    fun sendLike_success() {
        val receiverId = receiver.getIdOrThrow()
        val req = LikeRequest(receiverId)

        mockMvc.perform(
            post("$baseUrl/likes")
                .header("Authorization", bearer(senderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isMatched").value(false))

        val senderId = sender.getIdOrThrow()
        assertThat(matchRepository.findBySenderIdAndReceiverId(senderId, receiverId))
            .isPresent
    }

    @Test
    @DisplayName("상호 좋아요 보내기 성공 (REQUEST로 업그레이드)")
    fun sendLike_mutual_success() {
        // 먼저 receiver가 sender에게 좋아요를 보낸 상태
        matchRepository.save(Match.createLike(receiver, sender, BigDecimal.valueOf(0.8)))

        // sender가 receiver에게 좋아요를 보냄
        val receiverId = receiver.getIdOrThrow()
        val req = LikeRequest(receiverId)

        mockMvc.perform(
            post("$baseUrl/likes")
                .header("Authorization", bearer(senderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isMatched").value(true))

        // 기존 Match가 REQUEST 타입으로 변경되었는지 확인
        val senderId = sender.getIdOrThrow()
        val match = matchRepository.findMatchBetweenUsers(senderId, receiverId)
            .orElseThrow { IllegalStateException("매칭을 찾을 수 없습니다.") }
        assertThat(match.matchType).isEqualTo(MatchType.REQUEST)
    }

    @Test
    @DisplayName("좋아요 취소 성공")
    fun cancelLike_success() {
        matchRepository.save(Match.createLike(sender, receiver, BigDecimal.valueOf(0.8)))

        val receiverId = receiver.getIdOrThrow()

        mockMvc.perform(
            delete("$baseUrl/$receiverId")
                .header("Authorization", bearer(senderToken))
        )
            .andExpect(status().isNoContent)

        val senderId = sender.getIdOrThrow()
        assertThat(matchRepository.findBySenderIdAndReceiverId(senderId, receiverId))
            .isEmpty
    }

    @Test
    @DisplayName("REQUEST 상태 → 한쪽 accept 성공")
    fun confirmMatch_oneSideAccept_success() {
        val m = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85))
        matchRepository.save(m)

        val matchId = m.getIdOrThrow()
        val req = MatchConfirmRequest("accept")

        mockMvc.perform(
            put("$baseUrl/$matchId/confirm")
                .header("Authorization", bearer(senderToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.matchStatus").value("PENDING"))
            .andExpect(jsonPath("$.message").value("룸메이트 매칭이 최종 확정되었습니다."))

        val updated = matchRepository.findById(matchId)
            .orElseThrow { IllegalStateException("매칭을 찾을 수 없습니다.") }
        assertThat(updated.senderResponse).isEqualTo(MatchStatus.ACCEPTED)
    }

    @Test
    @DisplayName("양쪽 accept → 최종 ACCEPTED")
    fun confirmMatch_bothAccept_success() {
        val m = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85))
        matchRepository.save(m)

        val matchId = m.getIdOrThrow()
        val req = MatchConfirmRequest("accept")

        mockMvc.perform(
            put("$baseUrl/$matchId/confirm")
                .header("Authorization", bearer(senderToken))
                .content(objectMapper.writeValueAsString(req))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            put("$baseUrl/$matchId/confirm")
                .header("Authorization", bearer(receiverToken))
                .content(objectMapper.writeValueAsString(req))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.matchStatus").value("ACCEPTED"))
    }

    @Test
    @DisplayName("결과 조회 성공 (ACCEPTED만)")
    fun getMatchResults_success() {
        val accepted = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.75))

        val senderId = sender.getIdOrThrow()
        val receiverId = receiver.getIdOrThrow()

        accepted.processUserResponse(senderId, MatchStatus.ACCEPTED)
        accepted.processUserResponse(receiverId, MatchStatus.ACCEPTED)
        matchRepository.save(accepted)

        val pending = Match.createRequest(sender, thirdUser, BigDecimal.valueOf(0.60))
        matchRepository.save(pending)

        mockMvc.perform(
            get("$baseUrl/results")
                .header("Authorization", bearer(senderToken))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].matchStatus").value("ACCEPTED"))
    }

    @Test
    @DisplayName("인증 없이 접근 → 401")
    fun unauthorized_fail() {
        mockMvc.perform(
            get("$baseUrl/status")
        ).andExpect(status().isUnauthorized)
    }
}