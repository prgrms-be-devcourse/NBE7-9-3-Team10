package com.unimate.domain.match.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import com.unimate.domain.userProfile.entity.UserProfile
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.StringQuery
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import kotlin.math.abs
import kotlin.math.round

@Service
class SimilarityCalculator(private val elasticsearchOperations: ElasticsearchOperations) {

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
        if (preference == null || profile == null) return 0.0

        val smokerScore = calculateBooleanScore(preference.isSmoker, profile.isSmoker)
        val sleepScore = calculateIntegerScore(preference.sleepTime, profile.sleepTime)
        val petScore = calculateBooleanScore(preference.isPetAllowed, profile.isPetAllowed)
        val ageGapScore = calculateAgeGapScore(preference.preferredAgeGap, profile.user.birthDate)
        
        val cleaningFrequencyScore = calculateIntegerScore(preference.cleaningFrequency, profile.cleaningFrequency)
        val hygieneLevelScore = calculateIntegerScore(preference.hygieneLevel, profile.hygieneLevel)
        val cleanlinessScore = (cleaningFrequencyScore + hygieneLevelScore) / 2.0

        val noiseSensitivityScore = calculateIntegerScore(preference.noiseSensitivity, profile.noiseSensitivity)
        val snoringScore = calculateBooleanScore(preference.isSnoring, profile.isSnoring)
        val noiseScore = (noiseSensitivityScore + snoringScore) / 2.0

        val drinkingFrequencyScore = calculateIntegerScore(preference.drinkingFrequency, profile.drinkingFrequency)
        val guestFrequencyScore = calculateIntegerScore(preference.guestFrequency, profile.guestFrequency)
        val lifestyleScore = (drinkingFrequencyScore + guestFrequencyScore) / 2.0

        val finalScore = (smokerScore * WEIGHT_SMOKING) +
                (sleepScore * WEIGHT_SLEEP) +
                (cleanlinessScore * WEIGHT_CLEANLINESS) +
                (ageGapScore * WEIGHT_AGE) +
                (noiseScore * WEIGHT_NOISE) +
                (petScore * WEIGHT_PET) +
                (lifestyleScore * WEIGHT_LIFESTYLE)

        return round(finalScore * 100) / 100.0
    }

    // Elasticsearch의 function_score를 사용하여 유사도 계산
    fun calculateSimilarityWithElasticsearch(preference: UserMatchPreference): Double {
        // 선호하는 연령대를 기반으로 검색할 생년월일 범위 계산
        val (minBirthDate, maxBirthDate) = preference.preferredAgeGap?.let {
            getBirthDateRangeFromAgeBlock(it)
        } ?: Pair(LocalDate.now().minusYears(100), LocalDate.now())

        // 스코어링 함수 배열과 정규화를 위한 총 가중치 계산
        val (functionsJson, totalWeight) = buildScoreFunctionsAndWeight(preference)

        // 총 가중치가 0이면 계산할 필요 없이 0점 반환
        if (totalWeight <= 0) return 0.0

        // 최종적으로 실행할 Elasticsearch function_score 쿼리 생성
        val query = """
        {
          "query": {
            "function_score": {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "matchingEnabled": true } },
                    { "range": { "user.birthDate": { "gte": "$minBirthDate", "lte": "$maxBirthDate" } } }
                  ]
                }
              },
              "functions": $functionsJson,
              "score_mode": "sum",
              "boost_mode": "replace" 
            }
          }
        }
        """.trimIndent()

        val searchQuery = StringQuery(query)
        val searchHits = elasticsearchOperations.search(searchQuery, UserProfile::class.java)

        if (searchHits.totalHits == 0L) return 0.0

        val rawScore = searchHits.getSearchHit(0).score.toDouble()
        // ES에서 받은 점수를 총 가중치로 나누어 0.0 ~ 1.0 사이 값으로 정규화
        val normalizedScore = rawScore / totalWeight
        
        return round(normalizedScore * 100.0) / 100.0
    }

    private fun buildScoreFunctionsAndWeight(preference: UserMatchPreference): Pair<String, Double> {
        val functions = mutableListOf<Map<String, Any>>()
        var totalWeight = 0.0

        fun addGaussFunction(field: String, origin: Int?, weight: Double) {
            origin?.let {
                functions.add(mapOf(
                    "gauss" to mapOf(
                        field to mapOf(
                            "origin" to it,
                            "scale" to "2",
                            "offset" to "0",
                            "decay" to 0.5
                        )
                    ),
                    "weight" to weight
                ))
                totalWeight += weight
            }
        }

        fun addBooleanFunction(field: String, value: Boolean?, weight: Double) {
            value?.let {
                functions.add(mapOf(
                    "filter" to mapOf("term" to mapOf(field to it)),
                    "weight" to weight
                ))
                totalWeight += weight
            }
        }

        addBooleanFunction("isSmoker", preference.isSmoker, WEIGHT_SMOKING)
        addBooleanFunction("isPetAllowed", preference.isPetAllowed, WEIGHT_PET)
        
        addGaussFunction("sleepTime", preference.sleepTime, WEIGHT_SLEEP)
        
        // 복합 점수 항목들은 가중치를 절반으로 나누어 각각의 함수로 추가
        addGaussFunction("cleaningFrequency", preference.cleaningFrequency, WEIGHT_CLEANLINESS / 2)
        addGaussFunction("hygieneLevel", preference.hygieneLevel, WEIGHT_CLEANLINESS / 2)
        
        addBooleanFunction("isSnoring", preference.isSnoring, WEIGHT_NOISE / 2)
        addGaussFunction("noiseSensitivity", preference.noiseSensitivity, WEIGHT_NOISE / 2)

        addGaussFunction("drinkingFrequency", preference.drinkingFrequency, WEIGHT_LIFESTYLE / 2)
        addGaussFunction("guestFrequency", preference.guestFrequency, WEIGHT_LIFESTYLE / 2)

        preference.preferredAgeGap?.let {
            val (minBirthDate, maxBirthDate) = getBirthDateRangeFromAgeBlock(it)
            functions.add(mapOf(
                "filter" to mapOf("range" to mapOf("user.birthDate" to mapOf("gte" to minBirthDate.toString(), "lte" to maxBirthDate.toString()))),
                "weight" to WEIGHT_AGE
            ))
            totalWeight += WEIGHT_AGE
        }

        val functionsJson = jacksonObjectMapper().writeValueAsString(functions)
        return Pair(functionsJson, totalWeight)
    }

    private fun getBirthDateRangeFromAgeBlock(ageBlock: Int): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (ageBlock) {
            1 -> Pair(now.minusYears(22).minusDays(1), now.minusYears(20))
            2 -> Pair(now.minusYears(25).minusDays(1), now.minusYears(23))
            3 -> Pair(now.minusYears(28).minusDays(1), now.minusYears(26))
            4 -> Pair(now.minusYears(30).minusDays(1), now.minusYears(29))
            5 -> Pair(now.minusYears(100), now.minusYears(31))
            else -> Pair(now.minusYears(100), now)
        }
    }

    private fun calculateIntegerScore(preferenceValue: Int?, profileValue: Int?): Double {
        if (preferenceValue == null || profileValue == null) return 0.0
        return 1.0 - (abs(preferenceValue - profileValue) / SCALE_RANGE)
    }

    private fun calculateBooleanScore(preferenceValue: Boolean?, profileValue: Boolean?): Double {
        if (preferenceValue == null || profileValue == null) return 0.0
        return if (preferenceValue == profileValue) 1.0 else 0.0
    }

    private fun calculateAgeGapScore(preferredAgeBlock: Int?, targetBirthDate: LocalDate?): Double {
        if (targetBirthDate == null || preferredAgeBlock == null) return 0.0
        val targetAge = Period.between(targetBirthDate, LocalDate.now()).years
        val targetAgeBlock = getAgeBlock(targetAge)
        if (targetAgeBlock == 0) return 0.0
        return calculateIntegerScore(preferredAgeBlock, targetAgeBlock)
    }

    private fun getAgeBlock(age: Int): Int {
        return when {
            age in 20..22 -> 1
            age in 23..25 -> 2
            age in 26..28 -> 3
            age in 29..30 -> 4
            age >= 31 -> 5
            else -> 0
        }
    }
}
