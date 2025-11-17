package com.unimate.domain.chatroom.repository

import com.unimate.domain.chatroom.entity.Chatroom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ChatroomRepository : JpaRepository<Chatroom, Long>, CustomChatroomRepository {

    fun existsBySmallerUserIdAndLargerUserId(
        smallerUserId: Long,
        largerUserId: Long
    ): Boolean

    fun findBySmallerUserIdAndLargerUserId(
        smallerUserId: Long,
        largerUserId: Long
    ): Optional<Chatroom>

    fun findByUser1IdOrUser2IdOrderByLastMessageAtDesc(
        user1Id: Long,
        user2Id: Long,
        pageable: Pageable
    ): Page<Chatroom>

    @Query(
        "SELECT c FROM Chatroom c WHERE " +
                "(c.user1Id = :userId AND c.user1Status = 'ACTIVE') OR " +
                "(c.user2Id = :userId AND c.user2Status = 'ACTIVE')"
    )
    fun findActiveRoomsByUser(@Param("userId") userId: Long): List<Chatroom>
}