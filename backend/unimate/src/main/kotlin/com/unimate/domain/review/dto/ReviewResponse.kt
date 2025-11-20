package com.unimate.domain.review.dto

import com.unimate.domain.review.entity.Review
import com.unimate.global.exception.ServiceException
import java.time.LocalDateTime

data class ReviewResponse(
    val reviewId: Long,
    val matchId: Long,
    val reviewerId: Long,
    val reviewerName: String,
    val revieweeId: Long,
    val revieweeName: String,
    val rating: Int,
    val content: String?,
    val recommend: Boolean,
    val canRematch: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(review: Review): ReviewResponse {
            return ReviewResponse(
                reviewId = review.id ?: throw ServiceException.internalServerError("Review ID가 null입니다."),
                matchId = review.match.id ?: throw ServiceException.internalServerError("Match ID가 null입니다."),
                reviewerId = review.reviewer.id ?: throw ServiceException.internalServerError("Reviewer ID가 null입니다."),
                reviewerName = review.reviewer.name,
                revieweeId = review.reviewee.id ?: throw ServiceException.internalServerError("Reviewee ID가 null입니다."),
                revieweeName = review.reviewee.name,
                rating = review.rating,
                content = review.content,
                recommend = review.recommend,
                canRematch = review.canRematch,
                createdAt = review.createdAt ?: throw ServiceException.internalServerError("생성일시가 null입니다."),
                updatedAt = review.updatedAt ?: throw ServiceException.internalServerError("수정일시가 null입니다.")
            )
        }
    }
}