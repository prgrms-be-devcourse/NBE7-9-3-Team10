package com.unimate.domain.notification.service

import com.unimate.domain.notification.entity.Notification
import com.unimate.domain.notification.entity.NotificationType
import com.unimate.domain.notification.repository.NotificationRepository
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @Transactional
    fun createNotification(
        userId: Long,
        type: NotificationType,
        message: String,
        senderName: String?,
        senderId: Long?
    ) {
        val user = findUserOrThrow(userId)

        val notification = Notification(
            user = user,
            type = type,
            message = message,
            senderName = senderName,
            senderId = senderId,
            chatroomId = null
        )

        notificationRepository.save(notification)
        sendWebSocketNotification(notification)
    }

    @Transactional
    fun createChatNotification(
        userId: Long,
        type: NotificationType,
        message: String,
        senderName: String?,
        senderId: Long?,
        chatroomId: Long?
    ) {
        val user = findUserOrThrow(userId)

        val notification = Notification(
            user = user,
            type = type,
            message = message,
            senderName = senderName,
            senderId = senderId,
            chatroomId = chatroomId
        )

        notificationRepository.save(notification)
        sendWebSocketNotification(notification)
    }

    private fun sendWebSocketNotification(notification: Notification) {
        try {
            val payload = mutableMapOf<String, Any?>(
                "id" to notification.id,
                "type" to notification.type.name,
                "message" to notification.message,
                "senderName" to notification.senderName,
                "senderId" to notification.senderId,
                "chatroomId" to notification.chatroomId,
                "isRead" to notification.isRead,
                "createdAt" to notification.createdAt?.toString()
            )

            messagingTemplate.convertAndSendToUser(
                notification.user.id.toString(),
                "/queue/notifications",
                payload
            )
        } catch (_: Exception) {
            // WebSocket 전송 실패는 무시 (DB에는 이미 저장됨)
        }
    }

    fun getUserNotifications(userId: Long, pageable: Pageable): Page<Notification> {
        val user = findUserOrThrow(userId)
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
    }

    fun getUnreadCount(userId: Long): Long {
        val user = findUserOrThrow(userId)
        return notificationRepository.countByUserAndIsReadFalse(user)
    }

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ServiceException.notFound("알림을 찾을 수 없습니다.") }

        if (notification.user.id != userId) {
            throw ServiceException.forbidden("본인의 알림만 읽을 수 있습니다.")
        }

        notification.markAsRead()
    }

    @Transactional
    fun deleteNotification(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ServiceException.notFound("알림을 찾을 수 없습니다.") }

        if (notification.user.id != userId) {
            throw ServiceException.forbidden("본인의 알림만 삭제할 수 있습니다.")
        }

        notificationRepository.delete(notification)
    }

    @Transactional
    fun deleteNotificationBySender(receiverId: Long, type: NotificationType, senderId: Long) {
        val receiver = findUserOrThrow(receiverId)
        notificationRepository.deleteByUserAndTypeAndSenderId(receiver, type, senderId)
    }

    fun notificationExistsBySender(receiverId: Long, type: NotificationType, senderId: Long): Boolean {
        val receiver = findUserOrThrow(receiverId)
        return notificationRepository.findByUserAndTypeAndSenderId(receiver, type, senderId).isPresent
    }

    private fun findUserOrThrow(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { ServiceException.notFound("사용자를 찾을 수 없습니다.") }
}