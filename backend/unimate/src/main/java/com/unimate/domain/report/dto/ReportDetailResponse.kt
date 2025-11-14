package com.unimate.domain.report.dto

import com.unimate.domain.report.entity.Report
import com.unimate.domain.user.user.entity.User
import java.time.LocalDateTime

data class ReportDetailResponse(
    val reportId: Long,
    val reporterInfo: UserInfo,
    val reportedInfo: UserInfo,
    val category: String,
    val content: String,
    val status: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
    ) {
    data class UserInfo (
        val userId: Long?,
        val name: String,
        val email: String,
        val university: String?
    ) {
        companion object {
            // if문 죽이고 let이랑 ?: 으로 교체
            fun fromUser(user: User?): UserInfo {
                return user?.let {
                    UserInfo(
                        userId = it.id,
                        name = it.name,
                        email = it.email,
                        university = it.university
                    )
                } ?: UserInfo(
                    userId = null,
                    name = "탈퇴한 사용자",
                    email = "N/A",
                    university = "N/A"
                )
            }
        }
    }

    companion object {
        fun fromEntity(report: Report): ReportDetailResponse {
            return ReportDetailResponse(
                reportId = report.id!!,
                reporterInfo = UserInfo.fromUser(report.reporter),
                reportedInfo = UserInfo.fromUser(report.reported),
                category = report.category,
                content = report.content,
                status = report.reportStatus.name,
                createdAt = report.createdAt,
                updatedAt = report.updatedAt
            )
        }
    }

}
