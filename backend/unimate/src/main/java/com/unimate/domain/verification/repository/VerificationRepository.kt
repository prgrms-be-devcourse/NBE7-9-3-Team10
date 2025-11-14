package com.unimate.domain.verification.repository

import com.unimate.domain.verification.entity.Verification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface VerificationRepository : JpaRepository<Verification, Long> {

    fun findByEmail(email: String): Optional<Verification>

    fun deleteByEmail(email: String)
}