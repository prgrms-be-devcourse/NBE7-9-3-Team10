package com.unimate.domain.match.entity

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
            columnNames = ["sender_id", "receiver_id"]
        )
    ]
)
// TODO: 후기(Review) 연동 시 재매칭 기능을 허용하기 위해
// rematch_round 컬럼 활성화 및 유니크 제약 조건 확장 예정
// 현재는 단일 매칭만 허용 (동일 조합 중복 금지)
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

    // 재매칭 회차 (현재 미사용, 향후 후기 시스템과 연계 시 활용)
    @Column(name = "rematch_round", nullable = false)
    var rematchRound: Int = 0
) : BaseEntity() {

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
        require(response == MatchStatus.ACCEPTED || response == MatchStatus.REJECTED) {
            "응답은 ACCEPTED 또는 REJECTED만 가능합니다."
        }

        when (userId) {
            this.sender.id -> this.senderResponse = response
            this.receiver.id -> this.receiverResponse = response
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
            senderResponse == MatchStatus.ACCEPTED && receiverResponse == MatchStatus.ACCEPTED -> {
                this.matchStatus = MatchStatus.ACCEPTED
                this.confirmedAt = LocalDateTime.now()
            }
            senderResponse == MatchStatus.REJECTED || receiverResponse == MatchStatus.REJECTED -> {
                this.matchStatus = MatchStatus.REJECTED
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
        return when (userId) {
            this.sender.id -> this.senderResponse
            this.receiver.id -> this.receiverResponse
            else -> MatchStatus.PENDING
        }
    }

    // 재매칭 회차 설정 메서드
    //  public void setRematchRound(Integer rematchRound) {
    //      this.rematchRound = rematchRound;
    //  }
}