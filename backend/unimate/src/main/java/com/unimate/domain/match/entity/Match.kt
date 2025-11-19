package com.unimate.domain.match.entity

import com.unimate.domain.review.entity.Review
import com.unimate.domain.user.user.entity.User
import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "matches",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_match_sender_receiver",
            columnNames = ["sender_id", "receiver_id", "rematch_round"]
        )
    ]
)
/**
 * - Review와 연계하여 재매칭 기능
 * - rematch_round를 통해 동일 사용자 간 여러 회차 매칭 가능
 */
class Match(
    // 사용자 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "sender_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_match_sender")
    )
    var sender: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "receiver_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_match_receiver")
    )
    var receiver: User,

    // 매칭 타입 / 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", length = 10, nullable = false)
    var matchType: MatchType = MatchType.LIKE,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", length = 10, nullable = false)
    var matchStatus: MatchStatus = MatchStatus.PENDING,

    // 양방향 응답 추적 (sender와 receiver 각각의 응답 상태)
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_response", length = 10, nullable = false)
    var senderResponse: MatchStatus = MatchStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_response", length = 10, nullable = false)
    var receiverResponse: MatchStatus = MatchStatus.PENDING,

    @Column(name = "preference_score", precision = 3, scale = 2, nullable = false)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    var preferenceScore: BigDecimal = BigDecimal.ZERO,

    // 매칭 확정 시점
    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    // 재매칭 회차 (Review 시스템과 연계하여 재매칭 시 증가)
    @Column(name = "rematch_round", nullable = false)
    var rematchRound: Int = 0
) : BaseEntity() {

    // 한 매칭에 여러 후기가 있을 수 있음 (양방향 후기)
    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = false)
    var reviews: MutableList<Review> = mutableListOf()


    companion object {
        @JvmStatic
        fun createLike(
            sender: User,
            receiver: User,
            preferenceScore: BigDecimal
        ): Match {
            return Match(
                sender = sender,
                receiver = receiver,
                matchType = MatchType.LIKE,
                matchStatus = MatchStatus.PENDING,
                preferenceScore = preferenceScore
            )
        }

        @JvmStatic
        fun createRequest(
            sender: User,
            receiver: User,
            preferenceScore: BigDecimal
        ): Match {
            return Match(
                sender = sender,
                receiver = receiver,
                matchType = MatchType.REQUEST,
                matchStatus = MatchStatus.PENDING,
                preferenceScore = preferenceScore
            )
        }

        @JvmStatic
        fun createRematch(
            sender: User,
            receiver: User,
            preferenceScore: BigDecimal,
            rematchRound: Int
        ): Match {
            require(rematchRound > 0) {
                "재매칭 회차는 1 이상이어야 합니다."
            }
            return Match(
                sender = sender,
                receiver = receiver,
                matchType = MatchType.REQUEST,
                matchStatus = MatchStatus.PENDING,
                preferenceScore = preferenceScore,
                rematchRound = rematchRound
            )
        }
    }

    fun upgradeToRequest(requestSender: User, requestReceiver: User) {
        this.sender = requestSender
        this.receiver = requestReceiver
        this.matchType = MatchType.REQUEST
        // status는 PENDING으로 유지
    }

    /**
     * 사용자의 응답 처리 및 최종 상태 결정
     * @param userId 응답하는 사용자 ID
     * @param response 사용자의 응답 (ACCEPTED or REJECTED)
     */
    fun processUserResponse(userId: Long?, response: MatchStatus) {
        require(userId != null) {
            "userId는 null일 수 없습니다."
        }
        require(response == MatchStatus.ACCEPTED || response == MatchStatus.REJECTED) {
            "응답은 ACCEPTED 또는 REJECTED만 가능합니다."
        }

        when (userId) {
            this.sender.id -> {
                this.senderResponse = response
            }
            this.receiver.id -> {
                this.receiverResponse = response
            }
            else -> throw IllegalArgumentException("매칭 참여자가 아닙니다.")
        }

        // 최종 상태 결정 로직
        updateFinalStatus()
    }

    /**
     * 양쪽 응답을 기반으로 최종 매칭 상태 결정
     * - 둘 다 ACCEPTED → 최종 ACCEPTED
     * - 한쪽이라도 REJECTED → 최종 REJECTED
     * - 한쪽이라도 PENDING → 최종 PENDING 유지
     */
    private fun updateFinalStatus() {
        when {
            senderResponse == MatchStatus.REJECTED || receiverResponse == MatchStatus.REJECTED -> {
                this.matchStatus = MatchStatus.REJECTED
                this.confirmedAt = LocalDateTime.now()
            }
            senderResponse == MatchStatus.ACCEPTED && receiverResponse == MatchStatus.ACCEPTED -> {
                this.matchStatus = MatchStatus.ACCEPTED
                this.confirmedAt = LocalDateTime.now()
            }
            else -> {
                this.matchStatus = MatchStatus.PENDING
            }
        }
    }

    /**
     * 특정 유저 응답 여부
     */
    fun hasUserResponded(userId: Long?): Boolean {
        if (userId == null) return false 
        if (this.sender.id == null || this.receiver.id == null) return false

        return when (userId) {
            this.sender.id -> this.senderResponse != MatchStatus.PENDING
            this.receiver.id -> this.receiverResponse != MatchStatus.PENDING
            else -> false
        }
    }

    /**
     * 특정 사용자의 응답 상태 조회
     */
    fun getUserResponse(userId: Long?): MatchStatus {
        if (userId == null) return MatchStatus.PENDING
        if (this.sender.id == null || this.receiver.id == null) return MatchStatus.PENDING

        return when (userId) {
            this.sender.id -> this.senderResponse
            this.receiver.id -> this.receiverResponse
            else -> MatchStatus.PENDING
        }
    }

    /**
     * 재매칭 여부 확인
     */
    fun isRematch(): Boolean {
        return this.rematchRound > 0
    }

    /**
     * 다음 재매칭 회차 계산
     */
    fun getNextRematchRound(): Int {
        return this.rematchRound + 1
    }

}