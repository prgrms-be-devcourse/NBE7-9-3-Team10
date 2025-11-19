package com.unimate.domain.review.dto

data class PendingReviewResponse(
    val matchId: Long,
    val revieweeId: Long,
    val revieweeName: String,
    val revieweeUniversity: String,
    val matchEndDate: String, // "2024년 5월" 형식
    val canCreateReview: Boolean,
    val remainingDays: Long? // 후기 작성까지 남은 기간 (null이면 작성 가능)
) {

}