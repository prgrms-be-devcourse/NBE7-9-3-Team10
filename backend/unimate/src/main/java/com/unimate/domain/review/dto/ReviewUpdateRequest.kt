package com.unimate.domain.review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class ReviewUpdateRequest(
    @field:Min(1, message = "평점은 1 이상이어야 합니다.")
    @field:Max(5, message = "평점은 5 이하여야 합니다.")
    val rating: Int? = null,

    val content: String? = null,

    val recommend: Boolean? = null
)