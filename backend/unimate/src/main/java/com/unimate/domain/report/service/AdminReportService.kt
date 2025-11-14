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

        // var대신 val 쓰고 if문 대신 컬렉션이랑 고차 함수
        val spec = listOfNotNull(
            status?.takeIf { it.isNotBlank() }?.let { ReportSpecification.withStatus(ReportStatus.valueOf(it.uppercase())) },
            keyword?.takeIf { it.isNotBlank() }?.let { ReportSpecification.withKeyword(it) }
        ).reduceOrNull { acc, specification -> acc.and(specification) }

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

        if (report.reportStatus == ReportStatus.RESOLVED || report.reportStatus == ReportStatus.REJECTED) {
            throw ServiceException.conflict("이미 처리된 신고입니다.")
        }

        // if문 굳이 쓰지 않고 when 안에서 해결
        return when {
            // 피신고자가 이미 탈퇴한 경우
            report.reported == null -> {
                report.updateStatus(ReportStatus.RESOLVED)
                AdminReportActionResponse(report.id!!, ReportStatus.RESOLVED.name, "신고 대상자를 찾을 수 없어 신고만 처리되었습니다.")
            }
            // 신고 'REJECTED' 처리
            request.action == AdminReportActionRequest.ActionType.REJECT -> {
                report.updateStatus(ReportStatus.REJECTED)
                AdminReportActionResponse(report.id!!, ReportStatus.REJECTED.name, "신고가 반려 처리되었습니다.")
            }
            // 피신고자 '강제 탈퇴'처리
            request.action == AdminReportActionRequest.ActionType.DEACTIVATE -> {
                val reportedUser = report.reported!!

                // 탈퇴할 유저와 관련된 모든 신고에서 해당 유저 정보 null로 변경
                val relatedReports = reportRepository.findByReporterOrReported(reportedUser, reportedUser)
                relatedReports.forEach { r ->
                    if (r.reporter?.id == reportedUser.id) {
                        r.reporter = null
                    }
                    if (r.reported?.id == reportedUser.id) {
                        r.reported = null
                    }
                }

                // User를 참조하는 다른 자식 테이블 데이터들 우선 삭제
                matchRepository.deleteAllBySenderOrReceiver(reportedUser, reportedUser)
                userProfileRepository.deleteByUserId(reportedUser.id!!)
                userMatchPreferenceRepository.deleteByUserId(reportedUser.id!!)
                notificationRepository.deleteByUser(reportedUser)

                // 현재 처리 중인 신고 건 상태 'RESOLVED'로 변경
                report.updateStatus(ReportStatus.RESOLVED)

                // 모든 참조 정리 후, 최종적으로 유저 삭제
                userRepository.delete(reportedUser)

                AdminReportActionResponse(report.id!!, ReportStatus.RESOLVED.name, "신고 대상자 계정이 강제 탈퇴 처리되었습니다.")
            }
            else -> throw IllegalStateException("지원하지 않는 action 타입입니다: ${request.action}")
        }
    }
}