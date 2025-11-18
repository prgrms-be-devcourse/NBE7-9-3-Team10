package com.unimate.domain.chatroom.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "chatroom",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_chatroom_pair",
            columnNames = ["smaller_user_id", "larger_user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_chatroom_user1", columnList = "user1_id"),
        Index(name = "idx_chatroom_user2", columnList = "user2_id"),
        Index(name = "idx_chatroom_last_at", columnList = "last_message_at DESC")
    ]
)
class Chatroom(

    @Column(name = "user1_id", nullable = false)
    var user1Id: Long,

    @Column(name = "user2_id", nullable = false)
    var user2Id: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    var status: ChatroomStatus = ChatroomStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "user1_status", nullable = false, length = 10)
    var user1Status: ChatroomStatus = ChatroomStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "user2_status", nullable = false, length = 10)
    var user2Status: ChatroomStatus = ChatroomStatus.ACTIVE,

    @Column(name = "last_read_message_id_user1")
    var lastReadMessageIdUser1: Long? = null,

    @Column(name = "last_read_message_id_user2")
    var lastReadMessageIdUser2: Long? = null,

    @Column(
        name = "smaller_user_id",
        insertable = false,
        updatable = false,
        columnDefinition = "BIGINT GENERATED ALWAYS AS (LEAST(user1_id, user2_id))"
    )
    var smallerUserId: Long? = null,

    @Column(
        name = "larger_user_id",
        insertable = false,
        updatable = false,
        columnDefinition = "BIGINT GENERATED ALWAYS AS (GREATEST(user1_id, user2_id))"
    )
    var largerUserId: Long? = null,

    @Column(name = "last_message_id")
    var lastMessageId: Long? = null,

    @Column(name = "last_message_at")
    var lastMessageAt: LocalDateTime? = null,

    @Column(name = "blocked_by")
    var blockedBy: Long? = null,

    @Column(name = "blocked_at")
    var blockedAt: LocalDateTime? = null,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "updated_at",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    fun block(byUserId: Long) {
        status = ChatroomStatus.CLOSED
        blockedBy = byUserId
        blockedAt = LocalDateTime.now()
    }

    fun leave(userId: Long) {
        when (userId) {
            user1Id -> user1Status = ChatroomStatus.CLOSED
            user2Id -> user2Status = ChatroomStatus.CLOSED
        }
    }

    fun reactivate() {
        status = ChatroomStatus.ACTIVE
        user1Status = ChatroomStatus.ACTIVE
        user2Status = ChatroomStatus.ACTIVE
        blockedBy = null
        blockedAt = null
    }

    fun updateLastRead(userId: Long, messageId: Long?) {
        when (userId) {
            user1Id -> lastReadMessageIdUser1 = messageId
            user2Id -> lastReadMessageIdUser2 = messageId
        }
    }

    fun bumpLastMessage(messageId: Long?, sentAt: LocalDateTime?) {
        lastMessageId = messageId
        lastMessageAt = sentAt
    }

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        if (createdAt == null) createdAt = now
        if (updatedAt == null) updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(user1Id: Long, user2Id: Long): Chatroom {
            require(user1Id != user2Id) { "Self chat is not allowed." }
            return Chatroom(
                user1Id = user1Id,
                user2Id = user2Id
            )
        }
    }
}