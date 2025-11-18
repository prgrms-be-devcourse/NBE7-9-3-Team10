package com.unimate.domain.userBlock.service

import com.unimate.domain.userBlock.entity.UserBlock
import com.unimate.domain.userBlock.repository.UserBlockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserBlockService(
    private val userBlockRepository: UserBlockRepository
) {

    /**
     * 사용자를 차단합니다.
     */
    @Transactional
    fun blockUser(blockerId: Long, blockedId: Long): UserBlock {
        require(blockerId != blockedId) { "자기 자신을 차단할 수 없습니다." }

        val existing = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
        if (existing != null) {
            if (existing.active) {
                throw IllegalStateException("이미 차단한 사용자입니다.")
            }
            // 기존 기록 재활성화
            return userBlockRepository.save(
                UserBlock(
                    id = existing.id,
                    blockerId = existing.blockerId,
                    blockedId = existing.blockedId,
                    blockedAt = existing.blockedAt,
                    active = true
                )
            )
        }

        return userBlockRepository.save(
            UserBlock(
                blockerId = blockerId,
                blockedId = blockedId
            )
        )
    }

    /**
     * 차단을 해제합니다.
     */
    @Transactional
    fun unblockUser(blockerId: Long, blockedId: Long) {
        val userBlock = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
            ?: throw IllegalArgumentException("차단 기록을 찾을 수 없습니다.")

        if (!userBlock.active) {
            throw IllegalStateException("이미 차단 해제된 사용자입니다.")
        }


        userBlockRepository.save(
            UserBlock(
                id = userBlock.id,
                blockerId = userBlock.blockerId,
                blockedId = userBlock.blockedId,
                blockedAt = userBlock.blockedAt,
                active = false
            )
        )
    }

    /**
     * 차단 여부 확인
     */
    fun isBlocked(blockerId: Long, blockedId: Long): Boolean {
        return userBlockRepository.existsByBlockerIdAndBlockedIdAndActiveTrue(blockerId, blockedId)
    }

    /**
     * 내가 차단한 사용자 목록
     */
    fun getBlockedUsers(blockerId: Long): List<UserBlock> {
        return userBlockRepository.findAllByBlockerIdAndActiveTrue(blockerId)
    }
}