package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import java.math.BigDecimal
import java.time.LocalDateTime

data class MatchConfirmResponse(
    val id: Long? = null,
    val senderId: Long? = null,
    val receiverId: Long? = null,
    val matchType: MatchType? = null,
    val matchStatus: MatchStatus? = null,
    val preferenceScore: BigDecimal? = null,
    val confirmedAt: LocalDateTime? = null,
    val message: String? = null,
    val sender: SenderInfo? = null, // 발신자 정보
    val receiver: ReceiverInfo? = null // 수신자 정보
) {
    data class SenderInfo(
        val id: Long? = null,
        val name: String? = null,
        val email: String? = null,
        val university: String? = null
    )

    data class ReceiverInfo(
        val id: Long? = null,
        val name: String? = null,
        val age: Int? = null,
        val university: String? = null,
        val email: String? = null
    )
}