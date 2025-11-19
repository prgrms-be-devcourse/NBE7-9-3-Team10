package com.unimate.domain.review.controller

import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.review.dto.*
import com.unimate.domain.review.service.MatchRematchService
import com.unimate.domain.review.service.ReviewService
import com.unimate.global.exception.ServiceException
import com.unimate.global.jwt.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "ReviewController", description = "후기 API")
@SecurityRequirement(name = "BearerAuth")
class ReviewController(
    private val reviewService: ReviewService,
    private val matchRepository: MatchRepository,
    private val matchRematchService: MatchRematchService
) {

    /**
     * 후기 작성
     */
    @PostMapping
    @Operation(summary = "후기 작성")
    fun createReview(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @Valid @RequestBody request: ReviewCreateRequest
    ): ResponseEntity<ReviewResponse> {
        val review = reviewService.createReview(user.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(review)
    }

    /**
     * 후기 조회
     */
    @GetMapping("/{reviewId}")
    @Operation(summary = "후기 조회")
    fun getReview(@PathVariable reviewId: Long): ResponseEntity<ReviewResponse> {
        val review = reviewService.getReview(reviewId)
        return ResponseEntity.ok(review)
    }

    /**
     * 매칭별 후기 목록 조회
     */
    @GetMapping("/match/{matchId}")
    @Operation(summary = "매칭별 후기 목록 조회")
    fun getReviewsByMatch(@PathVariable matchId: Long): ResponseEntity<List<ReviewResponse>> {
        val reviews = reviewService.getReviewsByMatch(matchId)
        return ResponseEntity.ok(reviews)
    }

    /**
     * 대기 중인 후기 목록 조회
     */
    @GetMapping("/pending")
    @Operation(summary = "대기 중인 후기 목록 조회")
    fun getPendingReviews(
        @AuthenticationPrincipal user: CustomUserPrincipal
    ): ResponseEntity<List<PendingReviewResponse>> {
        val pendingReviews = reviewService.getPendingReviews(user.userId)
        return ResponseEntity.ok(pendingReviews)
    }

    /**
     * 후기 수정
     */
    @PutMapping("/{reviewId}")
    @Operation(summary = "후기 수정")
    fun updateReview(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @PathVariable reviewId: Long,
        @Valid @RequestBody request: ReviewUpdateRequest
    ): ResponseEntity<ReviewResponse> {
        val review = reviewService.updateReview(
            reviewId = reviewId,
            reviewerId = user.userId,
            rating = request.rating,
            content = request.content,
            recommend = request.recommend
        )
        return ResponseEntity.ok(review)
    }

    /**
     * 후기 삭제
     */
    @DeleteMapping("/{reviewId}")
    @Operation(summary = "후기 삭제")
    fun deleteReview(
        @AuthenticationPrincipal user: CustomUserPrincipal,
        @PathVariable reviewId: Long
    ): ResponseEntity<Void> {
        reviewService.deleteReview(reviewId, user.userId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 재매칭 가능 여부 확인
     */
    @GetMapping("/match/{matchId}/can-rematch")
    @Operation(summary = "재매칭 가능 여부 확인")
    fun canRematch(
        @AuthenticationPrincipal _user: CustomUserPrincipal,
        @PathVariable matchId: Long
    ): ResponseEntity<CanRematchResponse> {
        val match = matchRepository.findByIdWithUsers(matchId)
            ?: throw ServiceException.notFound("매칭을 찾을 수 없습니다.")

        val canRematch = matchRematchService.canRematch(match, requireReview = true)
        return ResponseEntity.ok(CanRematchResponse(canRematch))
    }
}