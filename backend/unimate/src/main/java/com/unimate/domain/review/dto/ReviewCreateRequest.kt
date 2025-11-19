package com.unimate.domain.review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ReviewCreateRequest(
    @field:NotNull(message = "매칭 ID는 필수입니다.")
    val matchId: Long,

    @field:NotNull(message = "평점은 필수입니다.")
    @field:Min(1, message = "평점은 1 이상이어야 합니다.")
    @field:Max(5, message = "평점은 5 이하여야 합니다.")
    val rating: Int,

    val content: String? = null,

    @field:NotNull(message = "추천 여부는 필수입니다.")
    val recommend: Boolean,
)