package com.unimate.domain.review.service

import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.review.dto.ReviewCreateRequest
import com.unimate.domain.review.entity.Review
import com.unimate.domain.review.repository.ReviewRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userProfile.entity.UserProfile
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceTest {

    @Autowired
    private lateinit var reviewService: ReviewService

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var matchRepository: MatchRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var userMatchPreferenceRepository: UserMatchPreferenceRepository

    private lateinit var reviewer: User
    private lateinit var reviewee: User
    private lateinit var match: Match

    @BeforeEach
    fun setUp() {
        reviewRepository.deleteAll()
        matchRepository.deleteAll()
        userMatchPreferenceRepository.deleteAll()
        userProfileRepository.deleteAll()
        userRepository.deleteAll()

        reviewer = createUser("reviewer@test.ac.kr", "리뷰어")
        reviewee = createUser("reviewee@test.ac.kr", "리뷰이")

        val endDate = LocalDate.now().minusDays(10)
        createUserProfile(reviewer, endDate)
        createUserProfile(reviewee, endDate)

        match = Match(
            sender = reviewer,
            receiver = reviewee,
            matchType = MatchType.LIKE,
            matchStatus = MatchStatus.ACCEPTED,
            confirmedAt = LocalDateTime.now().minusDays(100)
        )
        matchRepository.save(match)
    }

    @Test
    @DisplayName("후기 작성 성공")
    fun createReview_Success() {
        val request = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 5,
            content = "좋은 경험",
            recommend = true
        )

        val response = reviewService.createReview(reviewer.id!!, request)

        assertThat(response.rating).isEqualTo(5)
        assertThat(response.content).isEqualTo("좋은 경험")
        assertThat(response.recommend).isTrue()
        assertThat(response.canRematch).isTrue()

        val savedReview = reviewRepository.findAll().firstOrNull()
        assertThat(savedReview).isNotNull
        assertThat(savedReview?.rating).isEqualTo(5)
    }

    @Test
    @DisplayName("후기 작성 실패 - 매칭을 찾을 수 없음")
    fun createReview_Fail_MatchNotFound() {
        val request = ReviewCreateRequest(
            matchId = 99999L,
            rating = 5,
            content = "테스트",
            recommend = true
        )

        assertThatThrownBy {
            reviewService.createReview(reviewer.id!!, request)
        }.isInstanceOf(ServiceException::class.java)
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

        assertThatThrownBy {
            reviewService.createReview(reviewer.id!!, request)
        }.isInstanceOf(ServiceException::class.java)
    }

    @Test
    @DisplayName("후기 작성 실패 - 중복 작성")
    fun createReview_Fail_Duplicate() {
        val request = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 5,
            content = "첫 번째",
            recommend = true
        )

        reviewService.createReview(reviewer.id!!, request)

        assertThatThrownBy {
            reviewService.createReview(reviewer.id!!, request)
        }.isInstanceOf(ServiceException::class.java)
    }

    @Test
    @DisplayName("후기 조회 성공")
    fun getReview_Success() {
        val createRequest = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 4,
            content = "테스트 리뷰",
            recommend = true
        )

        val created = reviewService.createReview(reviewer.id!!, createRequest)
        val found = reviewService.getReview(created.reviewId)

        assertThat(found.reviewId).isEqualTo(created.reviewId)
        assertThat(found.rating).isEqualTo(4)
        assertThat(found.content).isEqualTo("테스트 리뷰")
    }

    @Test
    @DisplayName("후기 수정 성공")
    fun updateReview_Success() {
        val createRequest = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 3,
            content = "초기",
            recommend = false
        )

        val created = reviewService.createReview(reviewer.id!!, createRequest)
        val updated = reviewService.updateReview(
            reviewId = created.reviewId,
            reviewerId = reviewer.id!!,
            rating = 5,
            content = "수정됨",
            recommend = true
        )

        assertThat(updated.rating).isEqualTo(5)
        assertThat(updated.content).isEqualTo("수정됨")
        assertThat(updated.recommend).isTrue()
        assertThat(updated.canRematch).isTrue()
    }

    @Test
    @DisplayName("후기 삭제 성공")
    fun deleteReview_Success() {
        val createRequest = ReviewCreateRequest(
            matchId = match.id!!,
            rating = 4,
            content = "삭제될 리뷰",
            recommend = true
        )

        val created = reviewService.createReview(reviewer.id!!, createRequest)
        reviewService.deleteReview(created.reviewId, reviewer.id!!)

        val deleted = reviewRepository.findById(created.reviewId)
        assertThat(deleted).isEmpty
    }

    @Test
    @DisplayName("대기 중인 후기 목록 조회")
    fun getPendingReviews_Success() {
        val pendingReviews = reviewService.getPendingReviews(reviewer.id!!)

        assertThat(pendingReviews).isNotEmpty
        assertThat(pendingReviews[0].matchId).isEqualTo(match.id)
        assertThat(pendingReviews[0].revieweeName).isEqualTo("리뷰이")
        assertThat(pendingReviews[0].canCreateReview).isTrue()
    }

    @Test
    @DisplayName("재매칭 가능 여부 확인 - 양방향 추천")
    fun canRematch_Success_BothRecommended() {
        // 양방향 리뷰 작성 (둘 다 추천)
        val request1 = ReviewCreateRequest(match.id!!, 5, "좋아요", true)
        val request2 = ReviewCreateRequest(match.id!!, 5, "좋아요", true)

        reviewService.createReview(reviewer.id!!, request1)
        reviewService.createReview(reviewee.id!!, request2)

        val canRematch = reviewService.canRematch(match)
        assertThat(canRematch).isTrue()
    }

    @Test
    @DisplayName("재매칭 불가능 - 한쪽이 비추천")
    fun canRematch_Fail_OneNotRecommended() {
        val request1 = ReviewCreateRequest(match.id!!, 5, "좋아요", true)
        val request2 = ReviewCreateRequest(match.id!!, 3, "아니요", false)

        reviewService.createReview(reviewer.id!!, request1)
        reviewService.createReview(reviewee.id!!, request2)

        val canRematch = reviewService.canRematch(match)
        assertThat(canRematch).isFalse()
    }

    // Helper methods
    private fun createUser(email: String, name: String): User {
        val user = User(
            email = email,
            password = "encoded",
            name = name,
            gender = Gender.MALE,
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
}
