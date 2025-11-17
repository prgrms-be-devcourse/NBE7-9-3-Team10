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
import java.util.*

@Repository
interface MatchRepository : JpaRepository<Match, Long> {
    // 보낸 사람과 받는 사람 기준으로 매칭 기록 찾기
    fun findBySenderIdAndReceiverId(senderId: Long, receiverId: Long): Optional<Match>

    // 사용자 기준으로 모든 매칭 기록 찾기
    @Query("SELECT DISTINCT m FROM Match m " +
            "LEFT JOIN FETCH m.sender " +
            "LEFT JOIN FETCH m.receiver " +
            "WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    fun findBySenderIdOrReceiverWithUsers(@Param("userId") userId: Long): List<Match>

    // 보낸 사람과 받는 사람 기준으로 좋아요 기록 찾기
    fun findBySenderIdAndReceiverIdAndMatchType(
        senderId: Long,
        receiverId: Long,
        matchType: MatchType
    ): Optional<Match>

    // 양방향으로 두 사용자 간의 'LIKE' 기록을 찾는 메서드
    @Query("SELECT m FROM Match m WHERE m.matchType = 'LIKE' AND " +
            "((m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id))")
    fun findLikeBetweenUsers(
        @Param("user1Id") user1Id: Long,
        @Param("user2Id") user2Id: Long
    ): Optional<Match>

    // 양방향으로 두 사용자 간의 매칭 기록을 찾는 메서드
    @Query("SELECT m FROM Match m WHERE " +
            "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id)")
    fun findMatchBetweenUsers(
        @Param("user1Id") user1Id: Long,
        @Param("user2Id") user2Id: Long
    ): Optional<Match>

    // 매칭 취소 시 쓰레기 row 삭제
    @Modifying
    @Query("DELETE FROM Match m WHERE " +
            "(m.sender.id = :userId OR m.receiver.id = :userId) AND " +
            "NOT (m.matchType = :requestType AND m.matchStatus = :acceptedStatus)")
    fun deleteUnconfirmedMatchesByUserId(
        @Param("userId") userId: Long,
        @Param("requestType") requestType: MatchType,
        @Param("acceptedStatus") acceptedStatus: MatchStatus
    )

    fun deleteAllBySenderOrReceiver(sender: User, receiver: User)
}
