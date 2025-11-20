package com.unimate.domain.match.service

import com.unimate.domain.match.dto.CachedUserProfile
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MatchFilterService(
    private val matchUtilityService: MatchUtilityService
) {

    fun applyUniversityFilter(profile: CachedUserProfile, senderUniversity: String): Boolean {
        return profile.university == senderUniversity
    }

    fun applySleepPatternFilter(profile: CachedUserProfile, sleepPatternFilter: String?): Boolean {
        if (sleepPatternFilter == null || sleepPatternFilter.trim().isEmpty()) {
            return true
        }

        val sleepTime = profile.sleepTime

        return when (sleepPatternFilter.lowercase()) {
            "very_early" -> sleepTime == 5
            "early" -> sleepTime == 4
            "normal" -> sleepTime == 3
            "late" -> sleepTime == 2
            "very_late" -> sleepTime == 1
            else -> false
        }
    }

    fun applyAgeRangeFilter(profile: CachedUserProfile, ageRangeFilter: String?): Boolean {
        if (ageRangeFilter == null || ageRangeFilter.trim().isEmpty()) {
            return true
        }

        val age = matchUtilityService.calculateAge(profile.birthDate)

        return when (ageRangeFilter.lowercase()) {
            "20-22" -> age >= 20 && age <= 22
            "23-25" -> age >= 23 && age <= 25
            "26-28" -> age >= 26 && age <= 28
            "29-30" -> age >= 29 && age <= 30
            "31+" -> age >= 31
            else -> false
        }
    }

    fun applyCleaningFrequencyFilter(profile: CachedUserProfile, cleaningFrequencyFilter: String?): Boolean {
        if (cleaningFrequencyFilter == null || cleaningFrequencyFilter.trim().isEmpty()) {
            return true
        }

        val cleaningFrequency = profile.cleaningFrequency

        return when (cleaningFrequencyFilter.lowercase()) {
            "daily" -> cleaningFrequency == 5
            "several_times_weekly" -> cleaningFrequency == 4
            "weekly" -> cleaningFrequency == 3
            "monthly" -> cleaningFrequency == 2
            "rarely" -> cleaningFrequency == 1
            else -> false
        }
    }

    fun hasOverlappingPeriodByRange(profile: CachedUserProfile, startDate: LocalDate?, endDate: LocalDate?): Boolean {
        if (startDate == null || endDate == null) return true

        val start = profile.startUseDate
        val end = profile.endUseDate

        return !start.isAfter(endDate) && !end.isBefore(startDate)
    }
}