package com.unimate.domain.review.repository

import com.unimate.domain.match.entity.Match
import com.unimate.domain.review.entity.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReviewRepository : JpaRepository<Review, Long> {
    // 특정 매칭에 대한 모든 후기 조회
    @Query("""
        SELECT r FROM Review r
        LEFT JOIN FETCH r.reviewer
        LEFT JOIN FETCH r.reviewee
        WHERE r.match = :match
    """)
    fun findByMatch(@Param("match") match: Match): List<Review>

    // 특정 매칭과 작성자 기준으로 후기 조회
    @Query("""
        SELECT r FROM Review r
        LEFT JOIN FETCH r.reviewer
        LEFT JOIN FETCH r.reviewee 
        WHERE r.match = :match AND r.reviewer.id = :reviewerId
    """)
    fun findByMatchAndReviewerId(
        @Param("match") match: Match,
        @Param("reviewerId") reviewerId: Long
    ): Review?

    @Query("""
        SELECT r FROM Review r 
        LEFT JOIN FETCH r.reviewer 
        LEFT JOIN FETCH r.reviewee 
        LEFT JOIN FETCH r.match 
        WHERE r.id = :reviewId 
    """)
    fun findByIdWithRelations(@Param("reviewId") reviewId: Long): Review?
}