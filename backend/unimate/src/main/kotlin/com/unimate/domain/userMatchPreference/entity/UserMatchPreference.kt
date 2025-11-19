package com.unimate.domain.userMatchPreference.entity

import com.unimate.domain.user.user.entity.User
import com.unimate.domain.userMatchPreference.dto.MatchPreferenceRequest
import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

// 필드에 ? 추가한 이유 << 선택하지 않음이나 상관 없음 같은 확장성 생각해서 픽스. (ES 테스트 겸하여)
@Entity
@Table(name = "user_match_preference")
class UserMatchPreference(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    var startUseDate: LocalDate,
    var endUseDate: LocalDate,
    var sleepTime: Int?,
    var isPetAllowed: Boolean?,
    var isSmoker: Boolean?,
    var cleaningFrequency: Int?,
    var preferredAgeGap: Int?,
    var hygieneLevel: Int?,
    var isSnoring: Boolean?,
    var drinkingFrequency: Int?,
    var noiseSensitivity: Int?,
    var guestFrequency: Int?
) : BaseEntity() {

    fun update(dto: MatchPreferenceRequest) {
        this.startUseDate = dto.startUseDate
        this.endUseDate = dto.endUseDate
        this.sleepTime = dto.sleepTime
        this.isPetAllowed = dto.isPetAllowed
        this.isSmoker = dto.isSmoker
        this.cleaningFrequency = dto.cleaningFrequency
        this.preferredAgeGap = dto.preferredAgeGap
        this.hygieneLevel = dto.hygieneLevel
        this.isSnoring = dto.isSnoring
        this.drinkingFrequency = dto.drinkingFrequency
        this.noiseSensitivity = dto.noiseSensitivity
        this.guestFrequency = dto.guestFrequency
    }

    companion object {
        fun fromDto(user: User, dto: MatchPreferenceRequest): UserMatchPreference {
            return UserMatchPreference(
                user = user,
                startUseDate = dto.startUseDate,
                endUseDate = dto.endUseDate,
                sleepTime = dto.sleepTime,
                isPetAllowed = dto.isPetAllowed,
                isSmoker = dto.isSmoker,
                cleaningFrequency = dto.cleaningFrequency,
                preferredAgeGap = dto.preferredAgeGap,
                hygieneLevel = dto.hygieneLevel,
                isSnoring = dto.isSnoring,
                drinkingFrequency = dto.drinkingFrequency,
                noiseSensitivity = dto.noiseSensitivity,
                guestFrequency = dto.guestFrequency
            )
        }
    }
}
