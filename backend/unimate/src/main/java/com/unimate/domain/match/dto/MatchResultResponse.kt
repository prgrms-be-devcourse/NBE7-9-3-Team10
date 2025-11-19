package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import java.math.BigDecimal
import java.time.LocalDateTime

data class MatchResultResponse(
    val results: List<MatchResultItem> = emptyList()
) {
    data class MatchResultItem(
        val id: Long,
        val senderId: Long,
        val senderName: String,
        val receiverId: Long,
        val receiverName: String,
        val matchType: MatchType,
        val matchStatus: MatchStatus,
        val preferenceScore: BigDecimal,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        val confirmedAt: LocalDateTime,
        val rematchRound: Int = 0
    )
}