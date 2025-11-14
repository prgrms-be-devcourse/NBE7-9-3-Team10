package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.user.user.entity.Gender
import java.math.BigDecimal

data class MatchRecommendationResponse(
    val recommendations: List<MatchRecommendationItem> = emptyList()
) {
    data class MatchRecommendationItem(
        val receiverId: Long? = null,
        val name: String? = null,
        val university: String? = null,
        val studentVerified: Boolean? = null,
        val gender: Gender? = null,
        val age: Int? = null,
        val mbti: String? = null,
        val preferenceScore: BigDecimal? = null,
        val matchType: MatchType? = null,
        val matchStatus: MatchStatus? = null,
        
        // 추가 프로필 정보 (추천 목록에서 바로 표시)
        val sleepTime: Int? = null,
        val cleaningFrequency: Int? = null,
        val isSmoker: Boolean? = null,
        val startUseDate: String? = null,
        val endUseDate: String? = null
    )
}