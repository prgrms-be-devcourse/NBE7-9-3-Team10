package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import java.math.BigDecimal
import java.time.LocalDateTime

data class MatchListResponse(
    val matches: List<MatchListItem> = emptyList()
) {
    data class MatchListItem(
        val id: Long,
        val senderId: Long,
        val receiverId: Long,
        val matchType: MatchType,
        val matchStatus: MatchStatus,
        val preferenceScore: BigDecimal,
        val confirmedAt: LocalDateTime? = null,
        val message: String? = null, // 매칭 상태 메시지

        val sender: SenderInfo? = null, // 발신자 정보
        val receiver: ReceiverInfo? = null // 수신자 정보
    ) {
        data class SenderInfo(
            val id: Long,
            val name: String,
            val email: String,
            val university: String
        )

        data class ReceiverInfo(
            val id: Long,
            val name: String,
            val age: Int,
            val university: String,
            val email: String
        )
    }
}