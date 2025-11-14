package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import java.math.BigDecimal
import java.time.LocalDateTime

data class MatchResultResponse(
    val results: List<MatchResultItem> = emptyList()
) {
    data class MatchResultItem(
        val id: Long? = null,
        val senderId: Long? = null,
        val senderName: String? = null,
        val receiverId: Long? = null,
        val receiverName: String? = null,
        val matchType: MatchType? = null,
        val matchStatus: MatchStatus? = null,
        val preferenceScore: BigDecimal? = null,
        val createdAt: LocalDateTime? = null,
        val updatedAt: LocalDateTime? = null,
        val confirmedAt: LocalDateTime? = null
    )
}