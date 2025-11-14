package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.user.user.entity.Gender
import java.math.BigDecimal
import java.time.LocalDate

data class MatchRecommendationDetailResponse(
    val receiverId: Long,
    val email: String? = null, // 신고 기능을 위한 이메일 추가
    val name: String,
    val university: String,
    val studentVerified: Boolean,
    val mbti: String,
    val gender: Gender,
    val age: Int,

    val isSmoker: Boolean,
    val isPetAllowed: Boolean,
    val isSnoring: Boolean,

    val sleepTime: Int,
    val cleaningFrequency: Int,
    val hygieneLevel: Int,
    val noiseSensitivity: Int,
    val drinkingFrequency: Int,
    val guestFrequency: Int,
    val preferredAgeGap: Int,

    val birthDate: LocalDate,
    val startUseDate: LocalDate,
    val endUseDate: LocalDate,

    val preferenceScore: BigDecimal? = null,
    val matchType: MatchType? = null,
    val matchStatus: MatchStatus? = null
)