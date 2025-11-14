package com.unimate.domain.match.dto

import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import java.math.BigDecimal
import java.time.LocalDateTime

data class MatchStatusResponse(
    val matches: List<MatchStatusItem> = emptyList(), // 모든 매칭 목록
    val summary: SummaryInfo // 요약 정보
) {
    data class MatchStatusItem(
        val id: Long,
        val senderId: Long,
        val receiverId: Long,
        val matchType: MatchType,
        val matchStatus: MatchStatus,
        val preferenceScore: BigDecimal,
        val createdAt: LocalDateTime,
        val confirmedAt: LocalDateTime? = null,
        val message: String? = null, // 상태별 메시지

        // 양방향 응답 추적 필드
        val myResponse: MatchStatus,     // 현재 사용자의 응답 상태
        val partnerResponse: MatchStatus, // 상대방의 응답 상태
        val waitingForPartner: Boolean,    // 상대방의 응답 대기 중 여부

        val partner: PartnerInfo// 상대방 정보
    ) {
        data class PartnerInfo(
            val id: Long,
            val name: String,
            val email: String,
            val university: String
        )
    }

    data class SummaryInfo(
        val total: Int,    // 전체 매칭 수
        val pending: Int,  // 대기 중인 매칭 수
        val accepted: Int, // 수락된 매칭 수
        val rejected: Int  // 거절된 매칭 수
    )
}
