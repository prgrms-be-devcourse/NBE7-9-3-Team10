package com.unimate.domain.match.service

import com.unimate.domain.match.dto.CachedUserProfile
import com.unimate.domain.match.dto.LikeRequest
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
 class MatchServiceTest {

    @Autowired
    private lateinit var matchService: MatchService

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var userMatchPreferenceRepository: UserMatchPreferenceRepository

    @MockitoBean
    private lateinit var matchCacheService: MatchCacheService

    private lateinit var sender: User
    private lateinit var receiver: User

    private fun User.getIdOrThrow(): Long = id ?: error("User ID가 null입니다. 저장 후 ID가 생성되어야 합니다.")

    private fun Match.getIdOrThrow(): Long = id ?: error("Match ID가 null입니다. 저장 후 ID가 생성되어야 합니다.")


    @BeforeEach
    fun setUp() {
        matchRepository.deleteAll()
        userMatchPreferenceRepository.deleteAll()
        userProfileRepository.deleteAll()
        userRepository.deleteAll()

        sender = createUser("sender@test.ac.kr", "송신자", Gender.MALE)
        receiver = createUser("receiver@test.ac.kr", "수신자", Gender.MALE)

        createUserProfile(sender, true)
        createUserProfile(receiver, true)

        createUserPreference(sender)
        createUserPreference(receiver)
    }

    private fun createUser(email: String, name: String, gender: Gender): User {
        val user = User(
            name,
            email,
            "password123!",
            gender,
            LocalDate.now().minusYears(27),
            "서울대학교"
        )
        user.studentVerified = true
        return userRepository.save(user)
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
            3,
            true,
            false,
            3,
            2,
            3,
            false,
            1,
            2,
            1
        )
        userMatchPreferenceRepository.save(pref)
    }

    private fun createCachedUserProfile(user: User, profile: UserProfile): CachedUserProfile {
        val userId = user.getIdOrThrow()
        return CachedUserProfile(
            userId = userId,
            name = user.name,
            email = user.email,
            gender = user.gender,
            birthDate = user.birthDate,
            university = user.university,
            studentVerified = user.studentVerified,
            sleepTime = profile.sleepTime,
            isPetAllowed = profile.isPetAllowed,
            isSmoker = profile.isSmoker,
            cleaningFrequency = profile.cleaningFrequency,
            preferredAgeGap = profile.preferredAgeGap,
            hygieneLevel = profile.hygieneLevel,
            isSnoring = profile.isSnoring,
            drinkingFrequency = profile.drinkingFrequency,
            noiseSensitivity = profile.noiseSensitivity,
            guestFrequency = profile.guestFrequency,
            mbti = profile.mbti,
            startUseDate = profile.startUseDate,
            endUseDate = profile.endUseDate,
            matchingEnabled = profile.matchingEnabled
        )
    }

    @Test
    @DisplayName("매칭 상태 조회 성공")
    fun `getMatchStatus should return correct status`() {
        // given
        val match1 = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.8))
        matchRepository.save(match1)

        val senderId = sender.getIdOrThrow()

        // when
        val response = matchService.getMatchStatus(senderId)

        // then
        assertThat(response.summary.total).isEqualTo(1)
        assertThat(response.summary.pending).isEqualTo(1)
        assertThat(response.summary.accepted).isEqualTo(0)
        assertThat(response.summary.rejected).isEqualTo(0)
        assertThat(response.matches).hasSize(1)
    }

    @Test
    @DisplayName("매칭 결과 조회 성공 - ACCEPTED만 반환")
    fun `getMatchResults should return only ACCEPTED matches`() {
        // given
        val acceptedMatch = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.8))
        val senderId = sender.getIdOrThrow()
        val receiverId = receiver.getIdOrThrow()

        // 실제 프로세스처럼 양쪽 모두 ACCEPTED 처리
        acceptedMatch.processUserResponse(senderId, MatchStatus.ACCEPTED)
        acceptedMatch.processUserResponse(receiverId, MatchStatus.ACCEPTED)
        matchRepository.save(acceptedMatch)

        val pendingMatch = Match.createRequest(
            sender,
            createUser("third@test.ac.kr", "제삼자", Gender.MALE),
            BigDecimal.valueOf(0.7)
        )
        matchRepository.save(pendingMatch)

        // when
        val response = matchService.getMatchResults(senderId)

        // then
        assertThat(response.results).hasSize(1)
        assertThat(response.results[0].matchStatus).isEqualTo(MatchStatus.ACCEPTED)
    }

    @Test
    @DisplayName("매칭 추천 목록 조회 성공")
    fun `getMatchRecommendations should return recommendations successfully`() {
        // given
        val receiverId = receiver.getIdOrThrow()

        // matchCacheService 모킹 설정
        val receiverProfile = userProfileRepository.findByUserId(receiverId)
            .orElseThrow { error("수신자 프로필을 찾을 수 없습니다.") }
        val cachedProfile = createCachedUserProfile(receiver, receiverProfile)
        whenever(matchCacheService.getAllCandidates()).thenReturn(listOf(cachedProfile))

        // when
        val response = matchService.getMatchRecommendations(
            sender.email,
            null, null, null, null, null
        )

        // then
        assertThat(response.recommendations).isNotEmpty
        assertThat(response.recommendations.any { it.receiverId == receiverId }).isTrue
    }

    @Test
    @DisplayName("매칭 추천 상세 조회 성공")
    fun `getMatchRecommendationDetail should return detail successfully`() {
        // given
        val receiverId = receiver.getIdOrThrow()

        // matchCacheService 모킹 설정
        val receiverProfile = userProfileRepository.findByUserId(receiverId)
            .orElseThrow { error("수신자 프로필을 찾을 수 없습니다.") }
        val cachedProfile = createCachedUserProfile(receiver, receiverProfile)
        whenever(matchCacheService.getUserProfileById(receiverId)).thenReturn(cachedProfile)

        // when
        val response = matchService.getMatchRecommendationDetail(sender.email, receiverId)

        // then
        assertThat(response.receiverId).isEqualTo(receiverId)
        assertThat(response.name).isEqualTo(receiver.name)
        assertThat(response.email).isEqualTo(receiver.email)
        assertThat(response.university).isEqualTo(receiver.university)
    }

    @Test
    @DisplayName("좋아요 보내기 성공")
    fun `sendLike should create new like successfully`() {
        // given
        val senderId = sender.getIdOrThrow()
        val receiverId = receiver.getIdOrThrow()
        val request = LikeRequest(receiverId)

        // matchCacheService 모킹 설정
        val receiverProfile = userProfileRepository.findByUserId(receiverId)
            .orElseThrow { error("수신자 프로필을 찾을 수 없습니다.") }
        val cachedProfile = createCachedUserProfile(receiver, receiverProfile)
        whenever(matchCacheService.getUserProfileById(receiverId)).thenReturn(cachedProfile)

        // when
        val response = matchService.sendLike(request, senderId)

        // then
        assertThat(response.isMatched).isFalse
        assertThat(matchRepository.findAllBySenderIdAndReceiverId(senderId, receiverId)).isNotEmpty()
    }

    @Test
    @DisplayName("좋아요 보내기 실패 - 자기 자신에게 보내는 경우")
    fun `sendLike should fail when sending to self`() {
        // given
        val senderId = sender.getIdOrThrow()
        val request = LikeRequest(senderId)

        // when & then
        val exception = assertThrows<ServiceException> {
            matchService.sendLike(request, senderId)
        }
        assertThat(exception.message).isEqualTo("자기 자신에게 '좋아요'를 보낼 수 없습니다.")
    }

    @Test
    @DisplayName("좋아요 취소 성공")
    fun `cancelLike should delete like successfully`() {
        // given
        val senderId = sender.getIdOrThrow()
        val receiverId = receiver.getIdOrThrow()
        val like = Match.createLike(sender, receiver, BigDecimal.valueOf(0.8))
        matchRepository.save(like)

        // when
        matchService.cancelLike(senderId, receiverId)

        // then
        assertThat(matchRepository.findAllBySenderIdAndReceiverId(senderId, receiverId)).isEmpty()
    }

    @Test
    @DisplayName("좋아요 취소 실패 - 존재하지 않는 좋아요")
    fun `cancelLike should fail when like does not exist`() {
        // given
        val senderId = sender.getIdOrThrow()
        val receiverId = receiver.getIdOrThrow()

        // when & then
        val exception = assertThrows<ServiceException> {
            matchService.cancelLike(senderId, receiverId)
        }
        assertThat(exception.message).isEqualTo("취소할 '좋아요' 기록이 존재하지 않습니다.")
    }

    @Test
    @DisplayName("매칭 확정 성공 - 한쪽만 확정")
    fun `confirmMatch should accept match successfully`() {
        // given
        val match = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85))
        matchRepository.save(match)

        val matchId = match.getIdOrThrow()
        val senderId = sender.getIdOrThrow()

        // when
        val result = matchService.confirmMatch(matchId, senderId)

        // then
        assertThat(result.senderResponse).isEqualTo(MatchStatus.ACCEPTED)
        assertThat(result.matchStatus).isEqualTo(MatchStatus.PENDING) // 아직 한쪽만 확정
    }

    @Test
    @DisplayName("매칭 확정 실패 - 존재하지 않는 매칭")
    fun `confirmMatch should fail when match does not exist`() {
        // given
        val nonExistentMatchId = 999L
        val senderId = sender.getIdOrThrow()

        // when & then
        val exception = assertThrows<ServiceException> {
            matchService.confirmMatch(nonExistentMatchId, senderId)
        }
        assertThat(exception.message).isEqualTo("매칭을 찾을 수 없습니다.")
    }

    @Test
    @DisplayName("매칭 확정 실패 - 권한이 없는 사용자")
    fun `confirmMatch should fail when user has no permission`() {
        // given
        val thirdUser = createUser("third@test.ac.kr", "제삼자", Gender.MALE)
        val match = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85))
        matchRepository.save(match)

        val matchId = match.getIdOrThrow()
        val thirdUserId = thirdUser.getIdOrThrow()

        // when & then
        val exception = assertThrows<ServiceException> {
            matchService.confirmMatch(matchId, thirdUserId)
        }
        assertThat(exception.message).isEqualTo("룸메이트 확정 권한이 없습니다.")
    }

    @Test
    @DisplayName("매칭 거절 성공")
    fun `rejectMatch should reject match successfully`() {
        // given
        val match = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85))
        matchRepository.save(match)

        val matchId = match.getIdOrThrow()
        val senderId = sender.getIdOrThrow()

        // when
        val result = matchService.rejectMatch(matchId, senderId)

        // then
        assertThat(result.senderResponse).isEqualTo(MatchStatus.REJECTED)
        assertThat(result.matchStatus).isEqualTo(MatchStatus.REJECTED)
    }

    @Test
    @DisplayName("매칭 거절 실패 - 존재하지 않는 매칭")
    fun `rejectMatch should fail when match does not exist`() {
        // given
        val nonExistentMatchId = 999L
        val senderId = sender.getIdOrThrow()

        // when & then
        val exception = assertThrows<ServiceException> {
            matchService.rejectMatch(nonExistentMatchId, senderId)
        }
        assertThat(exception.message).isEqualTo("매칭을 찾을 수 없습니다.")
    }

    @Test
    @DisplayName("매칭 거절 실패 - 권한이 없는 사용자")
    fun `rejectMatch should fail when user has no permission`() {
        // given
        val thirdUser = createUser("third@test.ac.kr", "제삼자", Gender.MALE)
        val match = Match.createRequest(sender, receiver, BigDecimal.valueOf(0.85))
        matchRepository.save(match)

        val matchId = match.getIdOrThrow()
        val thirdUserId = thirdUser.getIdOrThrow()

        // when & then
        val exception = assertThrows<ServiceException> {
            matchService.rejectMatch(matchId, thirdUserId)
        }
        assertThat(exception.message).isEqualTo("룸메이트 확정 권한이 없습니다.")
    }
}