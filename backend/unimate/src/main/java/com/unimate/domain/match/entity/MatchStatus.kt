package com.unimate.domain.match.entity

enum class MatchStatus(
    val description: String
) {
    NONE("관계 없음"),
    PENDING("대기"),
    ACCEPTED("수락"),
    REJECTED("거절");
}