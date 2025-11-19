package com.unimate.domain.match.entity

enum class MatchType(
    val description: String
) {
    NONE("관계 없음"),
    LIKE("좋아요"),
    REQUEST("정식 룸메 신청");
}