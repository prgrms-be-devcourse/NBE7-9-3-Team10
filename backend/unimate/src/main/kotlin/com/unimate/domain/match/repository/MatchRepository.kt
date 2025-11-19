package com.unimate.domain.match.repository

import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.domain.match.entity.MatchType
import com.unimate.domain.user.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MatchRepository : JpaRepository<Match, Long> {

    /**
     * 특정 sender → receiver 조합의 모든 매칭 (재매칭 포함)
     */
    @Query("""
        SELECT m FROM Match m
        WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId
        ORDER BY m.id DESC
    """)
    fun findAllBySenderIdAndReceiverId(
        @Param("senderId") senderId: Long,
        @Param("receiverId") receiverId: Long
    ): List<Match>

    /**
     * 특정 sender → receiver + matchType 조합의 모든 매칭
     */
    @Query("""
        SELECT m FROM Match m
        WHERE m.sender.id = :senderId
          AND m.receiver.id = :receiverId
          AND m.matchType = :matchType
        ORDER BY m.id DESC
    """)
    fun findAllBySenderReceiverAndType(
        @Param("senderId") senderId: Long,
        @Param("receiverId") receiverId: Long,
        @Param("matchType") matchType: MatchType
    ): List<Match>

    /**
     * 특정 유저의 모든 매칭(보낸/받은) + sender/receiver fetch join
     */
    @Query("""
        SELECT DISTINCT m FROM Match m
        LEFT JOIN FETCH m.sender
        LEFT JOIN FETCH m.receiver
        WHERE m.sender.id = :userId OR m.receiver.id = :userId
    """)
    fun findBySenderIdOrReceiverWithUsers(@Param("userId") userId: Long): List<Match>

    /**
     * 단일 매칭 조회 (유저 포함)
     */
    @Query("""
        SELECT m FROM Match m
        LEFT JOIN FETCH m.sender
        LEFT JOIN FETCH m.receiver
        WHERE m.id = :matchId
    """)
    fun findByIdWithUsers(@Param("matchId") matchId: Long): Match?

    /**
     * 양방향 LIKE 매칭 전체 조회
     */
    @Query("""
        SELECT m FROM Match m
        WHERE m.matchType = 'LIKE'
          AND (
                (m.sender.id = :user1Id AND m.receiver.id = :user2Id)
             OR (m.sender.id = :user2Id AND m.receiver.id = :user1Id)
          )
        ORDER BY m.id DESC
    """)
    fun findAllLikesBetweenUsers(
        @Param("user1Id") user1Id: Long,
        @Param("user2Id") user2Id: Long
    ): List<Match>

    /**
     * 두 유저 사이의 모든 매칭 기록(최신순)
     */
    @Query("""
        SELECT m FROM Match m
        WHERE (m.sender.id = :user1Id AND m.receiver.id = :user2Id)
           OR (m.sender.id = :user2Id AND m.receiver.id = :user1Id)
        ORDER BY m.id DESC
    """)
    fun findAllMatchesBetweenUsers(
        @Param("user1Id") user1Id: Long,
        @Param("user2Id") user2Id: Long
    ): List<Match>

    /**
     * 양방향으로 두 사용자 간의 매칭 기록을 찾는 메서드
     */
    @Query(
        value = "SELECT * FROM matches WHERE (sender_id = :user1Id AND receiver_id = :user2Id) OR (sender_id = :user2Id AND receiver_id = :user1Id) ORDER BY id DESC LIMIT 1",
        nativeQuery = true
    )
    fun findMatchBetweenUsers(
        @Param("user1Id") user1Id: Long,
        @Param("user2Id") user2Id: Long
    ): Match?

    /**
     * 매칭 취소 시 불필요한 row 삭제 (정상 REQUEST+ACCEPT 만 유지)
     */
    @Modifying
    @Query("""
        DELETE FROM Match m
        WHERE (m.sender.id = :userId OR m.receiver.id = :userId)
          AND NOT (m.matchType = :requestType AND m.matchStatus = :acceptedStatus)
    """)
    fun deleteUnconfirmedMatchesByUserId(
        @Param("userId") userId: Long,
        @Param("requestType") requestType: MatchType,
        @Param("acceptedStatus") acceptedStatus: MatchStatus
    )

    /**
     *  유저 탈퇴 시 sender/receiver 전부 삭제
     */
    fun deleteAllBySenderOrReceiver(sender: User, receiver: User)
}
