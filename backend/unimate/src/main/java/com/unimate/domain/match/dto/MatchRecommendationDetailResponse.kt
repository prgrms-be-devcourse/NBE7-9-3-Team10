package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.user.user.entity.Gender
import java.math.BigDecimal
import java.time.LocalDate

data class MatchRecommendationDetailResponse(
    val receiverId: Long? = null,
    val email: String? = null, // 신고 기능을 위한 이메일 추가
    val name: String? = null,
    val university: String? = null,
    val studentVerified: Boolean? = null,
    val mbti: String? = null,
    val gender: Gender? = null,
    val age: Int? = null,

    val isSmoker: Boolean? = null,
    val isPetAllowed: Boolean? = null,
    val isSnoring: Boolean? = null,

    val sleepTime: Int? = null,
    val cleaningFrequency: Int? = null,
    val hygieneLevel: Int? = null,
    val noiseSensitivity: Int? = null,
    val drinkingFrequency: Int? = null,
    val guestFrequency: Int? = null,
    val preferredAgeGap: Int? = null,

    val birthDate: LocalDate? = null,
    val startUseDate: LocalDate? = null,
    val endUseDate: LocalDate? = null,

    val preferenceScore: BigDecimal? = null,
    val matchType: MatchType? = null,
    val matchStatus: MatchStatus? = null
)
