package com.unimate.domain.user.admin.entity

import com.unimate.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import lombok.Getter
import lombok.NoArgsConstructor

@Entity
@Getter
@NoArgsConstructor
class AdminUser(
    @Column(
        nullable = false,
        unique = true,
        length = 100
    ) val email: String,

    @Column(
        nullable = false,
        length = 255
    ) val password: String,

    @Column(
        nullable = false,
        length = 50
    ) val name: String
) : BaseEntity()
