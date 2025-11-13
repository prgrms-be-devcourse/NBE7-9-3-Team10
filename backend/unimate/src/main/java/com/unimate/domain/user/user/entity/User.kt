package com.unimate.domain.user.user.entity

import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "users")
class User @JvmOverloads constructor(
    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, unique = true, length = 100)
    var email: String,

    @Column(nullable = false, length = 100)
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val gender: Gender,

    @Column(nullable = false)
    val birthDate: LocalDate,

    @Column(nullable = false, length = 100)
    val university: String,

    @Column(nullable = false)
    var studentVerified: Boolean = false
) : BaseEntity() {

    fun updateName(newName: String) {
        this.name = newName
    }

    fun updateEmail(newEmail: String) {
        this.email = newEmail
    }

    fun verifyStudent() {
        this.studentVerified = true
    }
}