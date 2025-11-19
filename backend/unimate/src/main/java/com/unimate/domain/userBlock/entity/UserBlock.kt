package com.unimate.domain.userBlock.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "user_block",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_blocker_blocked", columnNames = ["blocker_id", "blocked_id"])
    ]
)
class UserBlock(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "blocker_id", nullable = false)
    val blockerId: Long,

    @Column(name = "blocked_id", nullable = false)
    val blockedId: Long,

    @Column(name = "active", nullable = false)
    val active: Boolean = true
) {

    fun deactivate(): UserBlock = UserBlock(
        id = this.id,
        blockerId = this.blockerId,
        blockedId = this.blockedId,
        active = false
    )
}