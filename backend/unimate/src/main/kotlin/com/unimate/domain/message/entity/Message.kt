package com.unimate.domain.message.entity

import com.unimate.domain.chatroom.entity.Chatroom
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "message",
    indexes = [
        Index(name = "idx_msg_room_id", columnList = "chatroom_id,id")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_msg_idempotent",
            columnNames = ["chatroom_id", "sender_id", "client_message_id"]
        )
    ]
)
class Message(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "chatroom_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_message_chatroom")
    )
    var chatroom: Chatroom,

    @Column(nullable = false)
    var senderId: Long,

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "client_message_id", nullable = false, length = 64)
    var clientMessageId: String,

    @CreationTimestamp
    @Column(
        nullable = false,
        updatable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    var createdAt: LocalDateTime? = null
)