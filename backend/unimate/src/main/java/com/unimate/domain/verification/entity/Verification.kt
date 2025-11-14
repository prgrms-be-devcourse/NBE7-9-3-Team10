package com.unimate.domain.verification.entity

import com.unimate.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import java.time.LocalDateTime

@Entity
class Verification(
    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(nullable = false, length = 6)
    var code: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    @Column
    var verifiedAt: LocalDateTime? = null
) : BaseEntity() {

    // ✅ Java 테스트 호환을 위한 보조 생성자
    constructor(email: String, code: String, expiresAt: LocalDateTime)
            : this(email, code, expiresAt, null)

    // JPA를 위한 protected 기본 생성자
    protected constructor() : this("", "", LocalDateTime.now(), null)

    val isVerified: Boolean
        get() = verifiedAt != null

    val isExpired: Boolean
        get() = LocalDateTime.now().isAfter(expiresAt)

    fun updateCode(code: String, newExpiresAt: LocalDateTime) {
        this.code = code
        this.expiresAt = newExpiresAt
        this.verifiedAt = null
    }

    fun markVerified() {
        this.verifiedAt = LocalDateTime.now()
    }
}