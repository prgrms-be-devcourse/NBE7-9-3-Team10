package com.unimate.domain.report.service

import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.notification.repository.NotificationRepository
import com.unimate.domain.report.dto.AdminReportActionRequest
import com.unimate.domain.report.dto.AdminReportActionResponse
import com.unimate.domain.report.dto.ReportDetailResponse
import com.unimate.domain.report.dto.ReportListResponse
import com.unimate.domain.report.dto.ReportSummary
import com.unimate.domain.report.entity.Report
import com.unimate.domain.report.entity.ReportStatus
import com.unimate.domain.report.repository.ReportRepository
import com.unimate.domain.report.repository.ReportSpecification
import com.unimate.domain.user.admin.repository.AdminRepository
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository,
    private val matchRepository: MatchRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userMatchPreferenceRepository: UserMatchPreferenceRepository,
    private val notificationRepository: NotificationRepository
) {

    // 각 메서드 로직에 관해, 잊어먹지 않기 위해 간략 주석

    // 요청한 user가 admin인지 확인하는 메서드
    private fun checkIsAdmin(adminId: Long) {
        adminRepository.findById(adminId)
            .orElseThrow { ServiceException.forbidden("관리자 권한이 필요합니다.") }
    }

    // 모든 신고 건을 페이징 및 검색 조건에 따라 조회하는 메서드
    fun getReports(adminId: Long, pageable: Pageable, status: String?, keyword: String?): ReportListResponse {
        checkIsAdmin(adminId)

        // Specification 객체 생성 (동적 쿼리)
        // 초기 상태
        var spec: Specification<Report> = Specification { root, _, _ -> null}

        // 검색 조건(status) 있으면, 상태 검색 조건 추가
        if (!status.isNullOrBlank()) {
            spec = spec.and(ReportSpecification.withStatus(ReportStatus.valueOf(status.uppercase())))
        }

        // 검색 조건(keyword) 있으면, 키워드 검색 조건 추가
        if (!keyword.isNullOrBlank()) {
            spec = spec.and(ReportSpecification.withKeyword(keyword))
        }

        // 생성된 Specification 조건으로 DB에서 신고 목록 페이징하여 조회
        val reportPage: Page<Report> = reportRepository.findAll(spec, pageable)

        // 조회된 Report 엔티티 페이지를 ReportSummary dto 페이지로 변환
        val dtoPage: Page<ReportSummary> = reportPage.map { report ->
            ReportSummary(
                reportId = report.id!!,
                reporterName = report.reporter?.name ?: "탈퇴한 사용자",
                reportedName = report.reported?.name ?: "탈퇴한 사용자",
                category = report.category,
                status = report.reportStatus.name,
                createdAt = report.createdAt
            )
        }

        // 반환
        return ReportListResponse(
            content = dtoPage.content,
            page = dtoPage.number,
            size = dtoPage.size,
            totalElements = dtoPage.totalElements,
            totalPages = dtoPage.totalPages
        )
    }

    // 상세 조회 메서드
    fun getReportDetail(adminId: Long, reportId: Long): ReportDetailResponse {
        checkIsAdmin(adminId)

        val report = reportRepository.findById(reportId)
            .orElseThrow { ServiceException.notFound("해당 ID의 신고를 찾을 수 없습니다 : $reportId") }

        return ReportDetailResponse.fromEntity(report)
    }

    // 신고 건 처리 메서드
    @Transactional
    fun processReportAction(adminId: Long, reportId: Long, request: AdminReportActionRequest): AdminReportActionResponse {
        checkIsAdmin(adminId)

        val report = reportRepository.findById(reportId)
            .orElseThrow { ServiceException.notFound("해당 ID의 신고를 찾을 수 없습니다 : $reportId") }

    }

}