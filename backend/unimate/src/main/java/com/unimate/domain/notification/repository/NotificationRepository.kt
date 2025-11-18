package com.unimate.domain.notification.repository

import com.unimate.domain.notification.entity.Notification
import com.unimate.domain.notification.entity.NotificationType
import com.unimate.domain.user.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<Notification>

    fun countByUserAndIsReadFalse(user: User): Long

    fun findByUserAndIsReadFalseOrderByCreatedAtDesc(user: User): List<Notification>

    fun deleteByUser(user: User)

    fun findByUserAndTypeAndSenderId(
        user: User,
        type: NotificationType,
        senderId: Long
    ): Optional<Notification>

    @Transactional
    fun deleteByUserAndTypeAndSenderId(
        user: User,
        type: NotificationType,
        senderId: Long
    )
}