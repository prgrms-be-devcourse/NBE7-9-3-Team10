package com.unimate.domain.userMatchPreference.repository

import com.unimate.domain.userMatchPreference.entity.UserMatchPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserMatchPreferenceRepository : JpaRepository<UserMatchPreference, Long> {
    fun findByUserId(userId: Long): Optional<UserMatchPreference>
    fun deleteByUserId(userId: Long)
}
