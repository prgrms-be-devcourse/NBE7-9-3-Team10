package com.unimate.domain.match.service

import com.unimate.domain.match.dto.CachedUserProfile
import com.unimate.domain.user.user.entity.Gender
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
class MatchFilterServiceTest {

    @Autowired
    private lateinit var matchFilterService: MatchFilterService

    private fun createCachedProfile(
        userId: Long = 1L,
        university: String = "서울대학교",
        sleepTime: Int = 3,
        birthDate: LocalDate = LocalDate.now().minusYears(25),
        cleaningFrequency: Int = 3,
        startUseDate: LocalDate = LocalDate.now(),
        endUseDate: LocalDate = LocalDate.now().plusMonths(6)
    ): CachedUserProfile {
        return CachedUserProfile(
            userId = userId,
            name = "테스트 사용자",
            email = "test@test.ac.kr",
            gender = Gender.MALE,
            birthDate = birthDate,
            university = university,
            studentVerified = true,
            sleepTime = sleepTime,
            isPetAllowed = true,
            isSmoker = false,
            cleaningFrequency = cleaningFrequency,
            preferredAgeGap = 2,
            hygieneLevel = 3,
            isSnoring = false,
            drinkingFrequency = 2,
            noiseSensitivity = 3,
            guestFrequency = 2,
            mbti = "INFP",
            startUseDate = startUseDate,
            endUseDate = endUseDate,
            matchingEnabled = true
        )
    }

    @Test
    @DisplayName("대학 필터 - 동일 대학인 경우 true 반환")
    fun `applyUniversityFilter should return true for same university`() {
        // given
        val profile = createCachedProfile(university = "서울대학교")
        val senderUniversity = "서울대학교"

        // when
        val result = matchFilterService.applyUniversityFilter(profile, senderUniversity)

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("대학 필터 - 다른 대학인 경우 false 반환")
    fun `applyUniversityFilter should return false for different university`() {
        // given
        val profile = createCachedProfile(university = "서울대학교")
        val senderUniversity = "연세대학교"

        // when
        val result = matchFilterService.applyUniversityFilter(profile, senderUniversity)

        // then
        assertThat(result).isFalse
    }

    @Test
    @DisplayName("수면 패턴 필터 - null인 경우 true 반환")
    fun `applySleepPatternFilter should return true when filter is null`() {
        // given
        val profile = createCachedProfile(sleepTime = 3)

        // when
        val result = matchFilterService.applySleepPatternFilter(profile, null)

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("수면 패턴 필터 - 빈 문자열인 경우 true 반환")
    fun `applySleepPatternFilter should return true when filter is empty`() {
        // given
        val profile = createCachedProfile(sleepTime = 3)

        // when
        val result = matchFilterService.applySleepPatternFilter(profile, "   ")

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("수면 패턴 필터 - normal 필터와 일치하는 경우 true 반환")
    fun `applySleepPatternFilter should return true for matching normal pattern`() {
        // given
        val profile = createCachedProfile(sleepTime = 3)

        // when
        val result = matchFilterService.applySleepPatternFilter(profile, "normal")

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("수면 패턴 필터 - early 필터와 일치하는 경우 true 반환")
    fun `applySleepPatternFilter should return true for matching early pattern`() {
        // given
        val profile = createCachedProfile(sleepTime = 4)

        // when
        val result = matchFilterService.applySleepPatternFilter(profile, "early")

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("수면 패턴 필터 - 일치하지 않는 경우 false 반환")
    fun `applySleepPatternFilter should return false for non-matching pattern`() {
        // given
        val profile = createCachedProfile(sleepTime = 3)

        // when
        val result = matchFilterService.applySleepPatternFilter(profile, "early")

        // then
        assertThat(result).isFalse
    }

    @Test
    @DisplayName("나이 범위 필터 - null인 경우 true 반환")
    fun `applyAgeRangeFilter should return true when filter is null`() {
        // given
        val profile = createCachedProfile(birthDate = LocalDate.now().minusYears(25))

        // when
        val result = matchFilterService.applyAgeRangeFilter(profile, null)

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("나이 범위 필터 - 26-28 범위에 포함되는 경우 true 반환")
    fun `applyAgeRangeFilter should return true for age in 26-28 range`() {
        // given
        val profile = createCachedProfile(birthDate = LocalDate.now().minusYears(27))

        // when
        val result = matchFilterService.applyAgeRangeFilter(profile, "26-28")

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("나이 범위 필터 - 26-28 범위에 포함되지 않는 경우 false 반환")
    fun `applyAgeRangeFilter should return false for age not in 26-28 range`() {
        // given
        val profile = createCachedProfile(birthDate = LocalDate.now().minusYears(25))

        // when
        val result = matchFilterService.applyAgeRangeFilter(profile, "26-28")

        // then
        assertThat(result).isFalse
    }

    @Test
    @DisplayName("나이 범위 필터 - 31+ 범위에 포함되는 경우 true 반환")
    fun `applyAgeRangeFilter should return true for age in 31+ range`() {
        // given
        val profile = createCachedProfile(birthDate = LocalDate.now().minusYears(35))

        // when
        val result = matchFilterService.applyAgeRangeFilter(profile, "31+")

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("청소 빈도 필터 - null인 경우 true 반환")
    fun `applyCleaningFrequencyFilter should return true when filter is null`() {
        // given
        val profile = createCachedProfile(cleaningFrequency = 3)

        // when
        val result = matchFilterService.applyCleaningFrequencyFilter(profile, null)

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("청소 빈도 필터 - weekly 필터와 일치하는 경우 true 반환")
    fun `applyCleaningFrequencyFilter should return true for matching weekly frequency`() {
        // given
        val profile = createCachedProfile(cleaningFrequency = 3)

        // when
        val result = matchFilterService.applyCleaningFrequencyFilter(profile, "weekly")

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("청소 빈도 필터 - 일치하지 않는 경우 false 반환")
    fun `applyCleaningFrequencyFilter should return false for non-matching frequency`() {
        // given
        val profile = createCachedProfile(cleaningFrequency = 3)

        // when
        val result = matchFilterService.applyCleaningFrequencyFilter(profile, "daily")

        // then
        assertThat(result).isFalse
    }

    @Test
    @DisplayName("기간 겹침 확인 - null인 경우 true 반환")
    fun `hasOverlappingPeriodByRange should return true when dates are null`() {
        // given
        val profile = createCachedProfile(
            startUseDate = LocalDate.now(),
            endUseDate = LocalDate.now().plusMonths(6)
        )

        // when
        val result = matchFilterService.hasOverlappingPeriodByRange(profile, null, null)

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("기간 겹침 확인 - 겹치는 경우 true 반환")
    fun `hasOverlappingPeriodByRange should return true for overlapping periods`() {
        // given
        val profile = createCachedProfile(
            startUseDate = LocalDate.now(),
            endUseDate = LocalDate.now().plusMonths(6)
        )
        val startDate = LocalDate.now().plusMonths(2)
        val endDate = LocalDate.now().plusMonths(8)

        // when
        val result = matchFilterService.hasOverlappingPeriodByRange(profile, startDate, endDate)

        // then
        assertThat(result).isTrue
    }

    @Test
    @DisplayName("기간 겹침 확인 - 겹치지 않는 경우 false 반환")
    fun `hasOverlappingPeriodByRange should return false for non-overlapping periods`() {
        // given
        val profile = createCachedProfile(
            startUseDate = LocalDate.now(),
            endUseDate = LocalDate.now().plusMonths(6)
        )
        val startDate = LocalDate.now().plusMonths(7)
        val endDate = LocalDate.now().plusMonths(12)

        // when
        val result = matchFilterService.hasOverlappingPeriodByRange(profile, startDate, endDate)

        // then
        assertThat(result).isFalse
    }
}