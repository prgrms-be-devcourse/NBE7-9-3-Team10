package com.unimate.domain.notification.entity

import com.unimate.domain.user.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class Notification(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: NotificationType,

    @Column(nullable = false, length = 500)
    var message: String,

    @Column(name = "sender_name", length = 100)
    var senderName: String? = null,

    @Column(name = "sender_id")
    var senderId: Long? = null,

    @Column(name = "chatroom_id")
    var chatroomId: Long? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
) {
    fun markAsRead() {
        this.isRead = true
    }
}