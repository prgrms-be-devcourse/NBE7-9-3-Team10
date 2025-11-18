package com.unimate.domain.userProfile.repository

import com.unimate.domain.userProfile.entity.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findByUserEmail(email: String): Optional<UserProfile>
    fun findByUserId(userId: Long): Optional<UserProfile>
    fun deleteByUserId(userId: Long)
    fun existsByUserEmail(email: String): Boolean
}
