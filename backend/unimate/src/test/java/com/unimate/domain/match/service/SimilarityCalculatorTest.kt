package com.unimate.domain.match.service

import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userProfile.entity.UserProfile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimilarityCalculatorTest {

    @Autowired
    private lateinit var similarityCalculator: SimilarityCalculator

    @Test
    @DisplayName("Kotlin 마이그레이션 후 boolean getter 호출 테스트 (완벽 일치)")
    fun `calculateSimilarity with Kotlin boolean getters should succeed`() {
        // given
        val preferenceUser = User("prefUser", "pref@test.ac.kr", "pw", Gender.FEMALE, LocalDate.of(2000, 1, 1), "Test Uni")
        var profileUser = User("profUser", "prof@test.ac.kr", "pw", Gender.FEMALE, LocalDate.of(2000, 1, 1), "Test Uni")

        // UserMatchPreference (Kotlin)
        val preference = UserMatchPreference(
            preferenceUser,
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            3, // sleepTime
            true, // isPetAllowed
            false, // isSmoker
            4, // cleaningFrequency
            1, // preferredAgeGap (20-22세 선호)
            4, // hygieneLevel
            false, // isSnoring
            2, // drinkingFrequency
            3, // noiseSensitivity
            2 // guestFrequency
        )

        // profileUser의 나이는 테스트 시점 기준 20대 초반으로 getAgeBlock(age)에서 1을 반환
        profileUser = User("profUser", "prof@test.ac.kr", "pw", Gender.FEMALE, LocalDate.now().minusYears(21), "Test Uni")

        // UserProfile (Kotlin)
        val profile = UserProfile(
            profileUser,
            3, // sleepTime
            true, // isPetAllowed
            false, // isSmoker
            4, // cleaningFrequency
            1, // preferredAgeGap
            4, // hygieneLevel
            false, // isSnoring
            2, // drinkingFrequency
            3, // noiseSensitivity
            2, // guestFrequency
            "INFP",
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            true
        )

        // when
        val similarity = similarityCalculator.calculateSimilarity(preference, profile)

        // then
        // 모든 값이 거의 일치하므로 점수는 1.0에 가까워야 함
        assertThat(similarity).isEqualTo(1.0)
    }

    @Test
    @DisplayName("선호도와 프로필이 완전히 다를 때 0점 반환 테스트")
    fun `calculateSimilarity with different values should return zero`() {
        // given
        val preferenceUser = User("prefUser2", "pref2@test.ac.kr", "pw", Gender.MALE, LocalDate.of(1995, 1, 1), "Test Uni")
        var profileUser = User("profUser2", "prof2@test.ac.kr", "pw", Gender.MALE, LocalDate.of(2005, 1, 1), "Test Uni")

        // UserMatchPreference (Kotlin) - 가장 높은 값 선호
        val preference = UserMatchPreference(
            preferenceUser,
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            5,      // sleepTime
            true,   // isPetAllowed
            false,  // isSmoker
            5,      // cleaningFrequency
            5,      // preferredAgeGap (31세 이상 선호)
            5,      // hygieneLevel
            false,  // isSnoring
            5,      // drinkingFrequency
            5,      // noiseSensitivity
            5       // guestFrequency
        )

        // profileUser의 나이는 20대 초반으로 getAgeBlock(age)에서 1을 반환
        profileUser = User("profUser2", "prof2@test.ac.kr", "pw", Gender.MALE, LocalDate.now().minusYears(21), "Test Uni")

        // UserProfile (Kotlin) - 가장 낮은 값 보유
        val profile = UserProfile(
            profileUser,
            1,      // sleepTime
            false,  // isPetAllowed
            true,   // isSmoker
            1,      // cleaningFrequency
            1,      // preferredAgeGap
            1,      // hygieneLevel
            true,   // isSnoring
            1,      // drinkingFrequency
            1,      // noiseSensitivity
            1,      // guestFrequency
            "ESTJ",
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            true
        )

        // when
        val similarity = similarityCalculator.calculateSimilarity(preference, profile)

        // then
        // 모든 값이 정반대이므로 점수는 0.0에 가까워야 함
        assertThat(similarity).isEqualTo(0.0)
    }
}
