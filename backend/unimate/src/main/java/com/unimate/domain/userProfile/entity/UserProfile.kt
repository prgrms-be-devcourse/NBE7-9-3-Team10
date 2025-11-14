package com.unimate.domain.userProfile.entity

import com.unimate.domain.match.dto.CachedUserProfile
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.userProfile.dto.ProfileCreateRequest
import com.unimate.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "user_profile",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_profile_user_id", columnNames = ["user_id"])]
)
class UserProfile (
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_user_profile_user")
    )
    var user: User,

    var sleepTime: Int,
    var isPetAllowed: Boolean,
    var isSmoker: Boolean,
    var cleaningFrequency: Int,
    var preferredAgeGap: Int,
    var hygieneLevel: Int,
    var isSnoring: Boolean,
    var drinkingFrequency: Int,
    var noiseSensitivity: Int,
    var guestFrequency: Int,
    var mbti: String,
    var startUseDate: LocalDate,
    var endUseDate: LocalDate,
    var matchingEnabled: Boolean
) : BaseEntity() {
    companion object {

        fun fromRequest(user: User, req: ProfileCreateRequest): UserProfile =
            UserProfile(
                user = user,
                sleepTime = req.sleepTime,
                isPetAllowed = req.isPetAllowed,
                isSmoker = req.isSmoker,
                cleaningFrequency = req.cleaningFrequency,
                preferredAgeGap = req.preferredAgeGap,
                hygieneLevel = req.hygieneLevel,
                isSnoring = req.isSnoring,
                drinkingFrequency = req.drinkingFrequency,
                noiseSensitivity = req.noiseSensitivity,
                guestFrequency = req.guestFrequency,
                mbti = req.mbti,
                startUseDate = req.startUseDate,
                endUseDate = req.endUseDate,
                matchingEnabled = req.matchingEnabled
            )

        fun of(
            user: User,
            sleepTime: Int,
            cleaningFrequency: Int,
            isSmoker: Boolean,
            isPetAllowed: Boolean,
            isSnoring: Boolean,
            preferredAgeGap: Int,
            hygieneLevel: Int,
            drinkingFrequency: Int,
            noiseSensitivity: Int,
            guestFrequency: Int,
            mbti: String,
            startUseDate: LocalDate,
            endUseDate: LocalDate,
            matchingEnabled: Boolean = true
        ): UserProfile =
            UserProfile(
                user = user,
                sleepTime = sleepTime,
                isPetAllowed = isPetAllowed,
                isSmoker = isSmoker,
                cleaningFrequency = cleaningFrequency,
                preferredAgeGap = preferredAgeGap,
                hygieneLevel = hygieneLevel,
                isSnoring = isSnoring,
                drinkingFrequency = drinkingFrequency,
                noiseSensitivity = noiseSensitivity,
                guestFrequency = guestFrequency,
                mbti = mbti,
                startUseDate = startUseDate,
                endUseDate = endUseDate,
                matchingEnabled = matchingEnabled
            )

        fun fromCached(user: User, cached: CachedUserProfile): UserProfile =
            of(
                user = user,
                sleepTime = cached.sleepTime,
                cleaningFrequency = cached.cleaningFrequency,
                isSmoker = cached.isSmoker,
                isPetAllowed = cached.isPetAllowed,
                isSnoring = cached.isSnoring,
                preferredAgeGap = cached.preferredAgeGap,
                hygieneLevel = cached.hygieneLevel,
                drinkingFrequency = cached.drinkingFrequency,
                noiseSensitivity = cached.noiseSensitivity,
                guestFrequency = cached.guestFrequency,
                mbti = cached.mbti,
                startUseDate = cached.startUseDate,
                endUseDate = cached.endUseDate,
                matchingEnabled = cached.matchingEnabled
            )
    }

    fun update(req: ProfileCreateRequest) {
        sleepTime         = req.sleepTime
        isPetAllowed      = req.isPetAllowed
        isSmoker          = req.isSmoker
        cleaningFrequency = req.cleaningFrequency
        preferredAgeGap   = req.preferredAgeGap
        hygieneLevel      = req.hygieneLevel
        isSnoring         = req.isSnoring
        drinkingFrequency = req.drinkingFrequency
        noiseSensitivity  = req.noiseSensitivity
        guestFrequency    = req.guestFrequency
        mbti              = req.mbti
        startUseDate      = req.startUseDate
        endUseDate        = req.endUseDate
        matchingEnabled   = req.matchingEnabled
    }

    fun updateMatchingStatus(matchingEnabled: Boolean) {
        this.matchingEnabled = matchingEnabled
    }
}

