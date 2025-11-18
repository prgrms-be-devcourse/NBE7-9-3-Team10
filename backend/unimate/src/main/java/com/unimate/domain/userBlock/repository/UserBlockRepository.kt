package com.unimate.domain.userBlock.repository

import com.unimate.domain.userBlock.entity.UserBlock
import org.springframework.data.jpa.repository.JpaRepository

interface UserBlockRepository : JpaRepository<UserBlock, Long> {
    fun existsByBlockerIdAndBlockedIdAndActiveTrue(
        blockerId: Long,
        blockedId: Long
    ): Boolean

    fun findByBlockerIdAndBlockedId(
        blockerId: Long,
        blockedId: Long
    ): UserBlock?

    fun findAllByBlockerIdAndActiveTrue(blockerId: Long): List<UserBlock>
}