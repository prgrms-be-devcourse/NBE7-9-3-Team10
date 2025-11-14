package com.unimate.domain.user.admin.repository

import com.unimate.domain.user.admin.entity.AdminUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AdminRepository : JpaRepository<AdminUser, Long> {
    fun findByEmail(email: String): Optional<AdminUser>
    fun existsByEmail(email: String): Boolean
}
