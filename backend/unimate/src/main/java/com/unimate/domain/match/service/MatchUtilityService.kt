package com.unimate.domain.match.service

import com.unimate.domain.match.dto.MatchResultResponse.MatchResultItem
import com.unimate.domain.match.dto.MatchStatusResponse.MatchStatusItem
import com.unimate.domain.match.dto.MatchStatusResponse.MatchStatusItem.PartnerInfo
import com.unimate.domain.match.entity.Match
import com.unimate.domain.match.entity.MatchStatus
import com.unimate.global.exception.ServiceException
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.max

/**
 * 매칭 관련 유틸리티 함수들을 담당하는 서비스
 */
@Service
class MatchUtilityService {
    /**
     * 나이 계산
     */
    fun calculateAge(birthDate: LocalDate): Int {
        val today = LocalDate.now()
        var age = today.year - birthDate.year

        // 생일이 아직 지나지 않았으면 1살 빼기
        if (today.dayOfYear < birthDate.dayOfYear) {
            age--
        }

        return max(0, age)
    }

    /**
     * 매칭 상태별 메시지 반환
     */
    fun getStatusMessage(status: MatchStatus): String {
        return when (status) {
            MatchStatus.NONE -> "관계 없음"
            MatchStatus.PENDING -> "매칭 대기 중입니다."
            MatchStatus.ACCEPTED -> "룸메이트 매칭이 성사되었습니다!"
            MatchStatus.REJECTED -> "매칭이 거절되었습니다."
        }
    }

    /**
     * Match를 MatchStatusItem으로 변환
     */
    fun toMatchStatusItem(match: Match, currentUserId: Long): MatchStatusItem {
        val message = getStatusMessage(match.matchStatus)

        // 현재 사용자 기준으로 상대방 정보 설정
        val partner = if (match.sender.id == currentUserId)
            match.receiver
        else
            match.sender

        // 현재 사용자와 상대방의 응답 상태 조회
        val myResponse = match.getUserResponse(currentUserId)
        val partnerResponse = if (match.sender.id == currentUserId) {
            match.receiverResponse
        } else {
            match.senderResponse
        }

        // 상대방의 응답 대기 중 여부 판단
        val waitingForPartner = (myResponse != MatchStatus.PENDING) && (partnerResponse == MatchStatus.PENDING)

        val partnerId = partner.id ?: throw ServiceException.internalServerError("상대방 ID가 null입니다.")

        val partnerInfo =
            PartnerInfo(
                partnerId,
                partner.name,
                partner.email,
                partner.university
            )

        val matchId = match.id ?: throw ServiceException.internalServerError("매칭 ID가 null입니다.")
        val senderId = match.sender.id ?: throw ServiceException.internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw ServiceException.internalServerError("수신자 ID가 null입니다.")

        return MatchStatusItem(
            matchId,
            senderId,
            receiverId,
            match.matchType,
            match.matchStatus,
            match.preferenceScore,
            match.createdAt ?: throw ServiceException.internalServerError("생성일시가 null입니다."),
            match.confirmedAt,
            message,
            myResponse,
            partnerResponse,
            waitingForPartner,
            partnerInfo
        )
    }

    /**
     * Match를 MatchResultItem으로 변환
     */
    fun toMatchResultItem(match: Match): MatchResultItem {
        val matchId = match.id ?: throw ServiceException.internalServerError("매칭 ID가 null입니다.")
        val senderId = match.sender.id ?: throw ServiceException.internalServerError("송신자 ID가 null입니다.")
        val receiverId = match.receiver.id ?: throw ServiceException.internalServerError("수신자 ID가 null입니다.")
        val senderName = match.sender.name
        val receiverName = match.receiver.name

        return MatchResultItem(
            matchId,
            senderId,
            senderName,
            receiverId,
            receiverName,
            match.matchType,
            match.matchStatus,
            match.preferenceScore,
            match.createdAt ?: throw ServiceException.internalServerError("생성일시가 null입니다."),
            match.updatedAt ?: throw ServiceException.internalServerError("수정일시가 null입니다."),
            match.confirmedAt ?: throw ServiceException.internalServerError("확정일시가 null입니다.")
        )
    }
}
