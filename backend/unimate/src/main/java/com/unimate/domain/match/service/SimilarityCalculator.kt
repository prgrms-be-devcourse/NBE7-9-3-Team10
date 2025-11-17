package com.unimate.domain.match.service

import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userProfile.entity.UserProfile
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import kotlin.math.abs
import kotlin.math.round

@Service
class SimilarityCalculator {

    companion object {
        private const val MAX_SCORE_SCALE = 5
        private const val MIN_SCORE_SCALE = 1
        private const val SCALE_RANGE = (MAX_SCORE_SCALE - MIN_SCORE_SCALE).toDouble()

        // 가중치
        private const val WEIGHT_SMOKING = 0.20
        private const val WEIGHT_SLEEP = 0.20
        private const val WEIGHT_CLEANLINESS = 0.20
        private const val WEIGHT_AGE = 0.10
        private const val WEIGHT_NOISE = 0.10
        private const val WEIGHT_PET = 0.10
        private const val WEIGHT_LIFESTYLE = 0.10
    }

    fun calculateSimilarity(preference: UserMatchPreference?, profile: UserProfile?): Double {
        // Null 체크: preference 또는 profile이 null이면 0.0 반환
        if (preference == null || profile == null) return 0.0

        // 흡연 점수
        val smokerScore = calculateBooleanScore(preference.isSmoker, profile.isSmoker)

        // 수면 점수
        val sleepScore = calculateIntegerScore(preference.sleepTime, profile.sleepTime)

        // 반려동물 점수
        val petScore = calculateBooleanScore(preference.isPetAllowed, profile.isPetAllowed)

        // 나이 차이 점수
        val ageGapScore = calculateAgeGapScore(preference.preferredAgeGap, profile.user.birthDate)

        // 청결 점수 (청소 빈도 + 위생 수준)
        val cleaningFrequencyScore = calculateIntegerScore(preference.cleaningFrequency, profile.cleaningFrequency)
        val hygieneLevelScore = calculateIntegerScore(preference.hygieneLevel, profile.hygieneLevel)
        val cleanlinessScore = (cleaningFrequencyScore + hygieneLevelScore) / 2.0

        // 소음 점수 (소음 민감도 + 코골이 여부)
        val noiseSensitivityScore = calculateIntegerScore(preference.noiseSensitivity, profile.noiseSensitivity)
        val snoringScore = calculateBooleanScore(preference.isSnoring, profile.isSnoring)
        val noiseScore = (noiseSensitivityScore + snoringScore) / 2.0

        // 생활방식 점수 (음주 빈도 + 방문자 빈도)
        val drinkingFrequencyScore = calculateIntegerScore(preference.drinkingFrequency, profile.drinkingFrequency)
        val guestFrequencyScore = calculateIntegerScore(preference.guestFrequency, profile.guestFrequency)
        val lifestyleScore = (drinkingFrequencyScore + guestFrequencyScore) / 2.0

        // 가중치 적용 및 최종 점수 계산
        val finalScore = (smokerScore * WEIGHT_SMOKING) +
                (sleepScore * WEIGHT_SLEEP) +
                (cleanlinessScore * WEIGHT_CLEANLINESS) +
                (ageGapScore * WEIGHT_AGE) +
                (noiseScore * WEIGHT_NOISE) +
                (petScore * WEIGHT_PET) +
                (lifestyleScore * WEIGHT_LIFESTYLE)

        // 소수점 둘째 자리까지 반올림
        return round(finalScore * 100) / 100.0
    }

    private fun calculateIntegerScore(preferenceValue: Int?, profileValue: Int?): Double {
        if (preferenceValue == null || profileValue == null) {
            return 0.0
        }
        // 선호도와 프로필 값이 얼마나 다른지를 계산. 점수가 높을수록 유사함.
        return 1.0 - (abs(preferenceValue - profileValue) / SCALE_RANGE)
    }

    private fun calculateBooleanScore(preferenceValue: Boolean?, profileValue: Boolean?): Double {
        if (preferenceValue == null || profileValue == null) {
            return 0.0
        }
        // 선호도와 프로필 값이 일치하면 1점, 아니면 0점
        return if (preferenceValue == profileValue) 1.0 else 0.0
    }

    private fun calculateAgeGapScore(preferredAgeBlock: Int?, targetBirthDate: LocalDate?): Double {
        if (targetBirthDate == null || preferredAgeBlock == null) {
            return 0.0
        }

        val targetAge = Period.between(targetBirthDate, LocalDate.now()).years
        val targetAgeBlock = getAgeBlock(targetAge)

        if (targetAgeBlock == 0) { // 나이가 유효 범위 밖이면 0점
            return 0.0
        }

        // 나의 선호 블럭 번호와 상대의 실제 나이 블럭 번호를 비교하여 점수 계산
        return calculateIntegerScore(preferredAgeBlock, targetAgeBlock)
    }

    private fun getAgeBlock(age: Int): Int {
        return when {
            age in 20..22 -> 1
            age in 23..25 -> 2
            age in 26..28 -> 3
            age in 29..30 -> 4
            age >= 31 -> 5
            else -> 0 // 범위 밖의 나이는 0점 처리
        }
    }
}
