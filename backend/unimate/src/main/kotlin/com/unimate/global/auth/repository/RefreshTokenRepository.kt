package com.unimate.global.auth.repository

import com.unimate.global.auth.entity.RefreshToken
import com.unimate.global.auth.model.SubjectType
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findBySubjectTypeAndSubjectId(type: SubjectType, subjectId: Long): RefreshToken?

    fun findByRefreshToken(refreshToken: String): RefreshToken?
}