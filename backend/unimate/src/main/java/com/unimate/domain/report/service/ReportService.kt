package com.unimate.domain.report.service

import com.unimate.domain.report.dto.ReportCreateRequest
import com.unimate.domain.report.dto.ReportResponse
import com.unimate.domain.report.entity.Report
import com.unimate.domain.report.entity.ReportStatus
import com.unimate.domain.report.repository.ReportRepository
import com.unimate.domain.user.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(reporterEmail: String, rq: ReportCreateRequest): ReportResponse {
        val reporter = userRepository.findByEmail(reporterEmail)
            .orElseThrow { IllegalArgumentException("신고자 이메일 없음") }
        val reported = userRepository.findByEmail(rq.reportedEmail)
            .orElseThrow { IllegalArgumentException("피신고자 이메일 없음") }

        // 자신 신고 불가
        if (reporter.id == reported.id) {
            throw IllegalArgumentException("자신은 신고 불가")
        }

        val report = Report(
            reporter = reporter,
            reported = reported,
            category = rq.category,
            content = rq.content,
            reportStatus = ReportStatus.RECEIVED
        )
        val saved = reportRepository.save(report)

        return ReportResponse(
            reportId = saved.id!!,
            reporterEmail = reporterEmail,
            reportedEmail = rq.reportedEmail,
            category = saved.category,
            content = saved.content,
            reportStatus = saved.reportStatus,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt
        )
    }
}
