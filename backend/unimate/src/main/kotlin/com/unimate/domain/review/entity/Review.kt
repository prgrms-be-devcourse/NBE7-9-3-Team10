package com.unimate.domain.review.entity

import com.unimate.domain.match.entity.Match
import com.unimate.domain.user.user.entity.User
import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Entity
@Table(name = "reviews")
class Review(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: Match,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    val reviewer: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    var reviewee: User,

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    var rating: Int,

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    var content: String? = null,

    // 추천 여부 (UI: "룸메이트로 추천")
    @Column(name = "recommend", nullable = false)
    var recommend: Boolean = false,

    // 재매칭 관련 (UI: "룸메이트로 추천"과 연계)
    @Column(name = "can_rematch", nullable = false)
    var canRematch: Boolean = false,
) : BaseEntity()