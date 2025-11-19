package com.unimate.domain.userMatchPreference.service

import com.unimate.domain.match.service.MatchCacheService
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.dto.MatchPreferenceRequest
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.review.repository.ReviewRepository

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserMatchPreferenceServiceTest {

    @Autowired
    private lateinit var userMatchPreferenceService: UserMatchPreferenceService

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
    private lateinit var matchCacheService: MatchCacheService

    private lateinit var testUser: User
    private lateinit var testUserProfile: UserProfile

    @BeforeEach
    fun setup() {
        reviewRepository.deleteAll()
        matchRepository.deleteAll()
        userMatchPreferenceRepository.deleteAll()
        userProfileRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            name = "testuser",
            email = "test@test.ac.kr",
            password = "password",
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            university = "Test University"
        )
        userRepository.save(testUser)

        testUserProfile = UserProfile(
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
            matchingEnabled = false
        )
        userProfileRepository.save(testUserProfile)
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
    @DisplayName("새로운 매칭 선호도 생성 성공")
    fun `create new match preference success`() {
        // given
        val request = createSampleRequest()

        // when
        testUser.id?.let { userId ->
            val response = userMatchPreferenceService.updateMyMatchPreferences(userId, request)

            // then
            assertThat(response.userId).isEqualTo(userId)
            assertThat(response.sleepTime).isEqualTo(request.sleepTime)
            assertThat(response.isSmoker).isEqualTo(request.isSmoker)
            assertThat(response.startUseDate).isEqualTo(request.startUseDate)

            val savedPreference = userMatchPreferenceRepository.findByUserId(userId).get()
            assertThat(savedPreference.sleepTime).isEqualTo(request.sleepTime)

            val updatedProfile = userProfileRepository.findByUserId(userId).get()
            assertThat(updatedProfile.matchingEnabled).isTrue

            verify(matchCacheService).evictAllCandidatesCache()
        }
    }

    @Test
    @DisplayName("기존 매칭 선호도 업데이트 성공")
    fun `update existing match preference success`() {
        // given
        val initialRequest = createSampleRequest()
        testUser.id?.let { userId ->
            userMatchPreferenceService.updateMyMatchPreferences(userId, initialRequest)

            val updateRequest = MatchPreferenceRequest(
                startUseDate = LocalDate.now().plusDays(10),
                endUseDate = LocalDate.now().plusMonths(8),
                sleepTime = 1,
                isPetAllowed = true,
                isSmoker = false,
                cleaningFrequency = 1,
                preferredAgeGap = 1,
                hygieneLevel = 1,
                isSnoring = false,
                drinkingFrequency = 1,
                noiseSensitivity = 1,
                guestFrequency = 1
            )

            // when
            val response = userMatchPreferenceService.updateMyMatchPreferences(userId, updateRequest)

            // then
            assertThat(response.sleepTime).isEqualTo(updateRequest.sleepTime)
            assertThat(response.isSmoker).isEqualTo(updateRequest.isSmoker)

            val updatedPreference = userMatchPreferenceRepository.findByUserId(userId).get()
            assertThat(updatedPreference.sleepTime).isEqualTo(updateRequest.sleepTime)
            assertThat(updatedPreference.isSmoker).isEqualTo(updateRequest.isSmoker)

            verify(matchCacheService, times(2)).evictAllCandidatesCache()
        }
    }

    @Test
    @DisplayName("존재하지 않는 유저로 요청 시 실패")
    fun `update preferences with non-existing user fails`() {
        // given
        val nonExistingUserId = 999L
        val request = createSampleRequest()

        // when & then
        val exception = assertThrows<ServiceException> {
            userMatchPreferenceService.updateMyMatchPreferences(nonExistingUserId, request)
        }
        assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
    }

    @Test
    @DisplayName("프로필이 없는 유저로 요청 시 실패")
    fun `update preferences with no profile fails`() {
        // given
        val userWithoutProfile = User(
            name = "profiless",
            email = "profiless@test.ac.kr",
            password = "password",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(2001, 1, 1),
            university = "Test University"
        )
        userRepository.save(userWithoutProfile)
        val request = createSampleRequest()

        // when & then
        userWithoutProfile.id?.let { userId ->
            val exception = assertThrows<ServiceException> {
                userMatchPreferenceService.updateMyMatchPreferences(userId, request)
            }
            assertThat(exception.message).isEqualTo("해당 사용자의 프로필을 찾을 수 없습니다.")
        }
    }
}
