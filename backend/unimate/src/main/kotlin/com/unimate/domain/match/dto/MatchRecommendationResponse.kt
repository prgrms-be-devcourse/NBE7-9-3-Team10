package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.user.user.entity.Gender
import java.math.BigDecimal

data class MatchRecommendationResponse(
    val recommendations: List<MatchRecommendationItem> = emptyList()
) {
    data class MatchRecommendationItem(
        val receiverId: Long,
        val name: String,
        val university: String,
        val studentVerified: Boolean,
        val gender: Gender,
        val age: Int,
        val mbti: String,
        val preferenceScore: BigDecimal,
        val matchType: MatchType,
        val matchStatus: MatchStatus,

        // 추가 프로필 정보 (추천 목록에서 바로 표시)
        val sleepTime: Int,
        val cleaningFrequency: Int,
        val isSmoker: Boolean,
        val startUseDate: String,
        val endUseDate: String
    )
}