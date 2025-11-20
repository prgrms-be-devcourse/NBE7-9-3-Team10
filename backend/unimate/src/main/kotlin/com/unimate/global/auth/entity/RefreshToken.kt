package com.unimate.global.auth.entity

import com.unimate.global.auth.model.SubjectType
import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "refresh_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_subject_type_id", columnNames = ["subject_type", "subject_id"])
    ],
    indexes = [
        Index(name = "idx_refresh_token_value", columnList = "refresh_token")
    ]
)
class RefreshToken(
    @Enumerated(EnumType.STRING)
    @Column(
        name = "subject_type",
        nullable = false,
        length = 20
    )  val subjectType: SubjectType,

    @Column(
        name = "subject_id",
        nullable = false
    ) val subjectId: Long,

    @Column(
        name = "email",
        nullable = false,
        length = 255
    ) var email: String,

    @Column(
        name = "refresh_token",
        nullable = false,
        length = 512
    ) var refreshToken: String
) : BaseEntity() {
    fun updateToken(email: String, newToken: String) {
        this.email = email
        this.refreshToken = newToken
    }
}
