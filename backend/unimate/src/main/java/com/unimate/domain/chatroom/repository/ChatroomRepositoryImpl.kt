package com.unimate.domain.chatroom.repository

import com.unimate.domain.chatroom.entity.Chatroom
import com.unimate.domain.chatroom.entity.ChatroomStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ChatroomRepositoryImpl : CustomChatroomRepository {

    @PersistenceContext
    private lateinit var em: EntityManager

    override fun findRoomsByUserWithCursor(
        userId: Long,
        status: ChatroomStatus?,
        cursor: LocalDateTime?,
        limit: Int
    ): List<Chatroom> {
        val jpql = StringBuilder(
            """
            select c
              from Chatroom c
             where ((c.user1Id = :userId and c.user1Status = 'ACTIVE') or
                    (c.user2Id = :userId and c.user2Status = 'ACTIVE'))
            """.trimIndent()
        )

        if (status != null) {
            jpql.append(" and c.status = :status")
        }
        if (cursor != null) {
            jpql.append(" and coalesce(c.lastMessageAt, c.createdAt) < :cursor")
        }
        jpql.append(" order by coalesce(c.lastMessageAt, c.createdAt) desc")

        val query = em.createQuery(jpql.toString(), Chatroom::class.java)
            .setParameter("userId", userId)
            .setMaxResults(limit)

        if (status != null) query.setParameter("status", status)
        if (cursor != null) query.setParameter("cursor", cursor)

        return query.resultList
    }
}