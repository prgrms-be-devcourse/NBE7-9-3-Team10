package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import java.math.BigDecimal
import java.time.LocalDateTime

data class MatchStatusResponse(
    val matches: List<MatchStatusItem> = emptyList(), // 모든 매칭 목록
    val summary: SummaryInfo? = null // 요약 정보
) {
    data class MatchStatusItem(
        val id: Long? = null,
        val senderId: Long? = null,
        val receiverId: Long? = null,
        val matchType: MatchType? = null,
        val matchStatus: MatchStatus? = null,
        val preferenceScore: BigDecimal? = null,
        val createdAt: LocalDateTime? = null,
        val confirmedAt: LocalDateTime? = null,
        val message: String? = null, // 상태별 메시지

        // 양방향 응답 추적 필드
        val myResponse: MatchStatus? = null, // 현재 사용자의 응답 상태
        val partnerResponse: MatchStatus? = null, // 상대방의 응답 상태
        val waitingForPartner: Boolean = false, // 상대방의 응답 대기 중 여부

        val partner: PartnerInfo? = null // 상대방 정보
    ) {
        data class PartnerInfo(
            val id: Long? = null,
            val name: String? = null,
            val email: String? = null,
            val university: String? = null
        )
    }

    data class SummaryInfo(
        val total: Int = 0, // 전체 매칭 수
        val pending: Int = 0, // 대기 중인 매칭 수
        val accepted: Int = 0, // 수락된 매칭 수
        val rejected: Int = 0 // 거절된 매칭 수
    )
}
