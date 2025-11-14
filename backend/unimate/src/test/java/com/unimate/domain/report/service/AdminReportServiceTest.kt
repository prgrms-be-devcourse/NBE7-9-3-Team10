package com.unimate.domain.report.service

import com.unimate.domain.match.repository.MatchRepository
import com.unimate.domain.notification.repository.NotificationRepository
import com.unimate.domain.report.dto.AdminReportActionRequest
import com.unimate.domain.report.entity.Report
import com.unimate.domain.report.entity.ReportStatus
import com.unimate.domain.report.repository.ReportRepository
import com.unimate.domain.user.admin.entity.AdminUser
import com.unimate.domain.user.admin.repository.AdminRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import com.unimate.domain.userMatchPreference.repository.UserMatchPreferenceRepository
import com.unimate.domain.userProfile.repository.UserProfileRepository
import com.unimate.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class) // Mockito 기능을 JUnit5에서 사용
class AdminReportServiceTest {

    // @Mock: Mockito가 가짜(Mock) 객체를 생성
    @Mock private lateinit var reportRepository: ReportRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var adminRepository: AdminRepository
    @Mock private lateinit var matchRepository: MatchRepository
    @Mock private lateinit var userProfileRepository: UserProfileRepository
    @Mock private lateinit var userMatchPreferenceRepository: UserMatchPreferenceRepository
    @Mock private lateinit var notificationRepository: NotificationRepository

    // @InjectMocks: @Mock으로 만든 가짜 객체들을 실제 테스트 대상인 AdminReportService에 주입
    @InjectMocks
    private lateinit var adminReportService: AdminReportService

    // --- FIXTURE (테스트 데이터) ---
    private lateinit var admin: AdminUser
    private lateinit var reporter: User
    private lateinit var reported: User
    private lateinit var testReport: Report

    @BeforeEach
    fun setUp() {
        // 테스트에 사용할 공통 객체들을 실제 Kotlin 클래스의 주 생성자에 맞게 생성
        admin = AdminUser("admin@test.com", "password", "관리자").apply { id = 1L }
        reporter = User("신고자", "reporter@test.com", "password", Gender.MALE, LocalDate.now(), "서울대").apply { id = 2L }
        reported = User("피신고자", "reported@test.com", "password", Gender.FEMALE, LocalDate.now(), "연세대").apply { id = 3L }

        // Report 엔티티는 주 생성자를 사용
        testReport = Report(
            reporter = reporter,
            reported = reported,
            category = "욕설",
            content = "내용",
            reportStatus = ReportStatus.RECEIVED
        ).apply { id = 100L }
    }

    // --- TEST CODE ---
    @Test
    @DisplayName("신고 목록 조회 성공")
    fun t1_getReportsSuccess() {
        // given
        val pageable = PageRequest.of(0, 10)
        val reportPage = PageImpl(listOf(testReport), pageable, 1)

        whenever(adminRepository.findById(admin.id!!)).thenReturn(Optional.of(admin))
        whenever(reportRepository.findAll(any<Specification<Report>>(), any<Pageable>())).thenReturn(reportPage)

        // when
        val result = adminReportService.getReports(admin.id!!, pageable, null, null)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].reportId).isEqualTo(testReport.id)
        verify(adminRepository).findById(admin.id!!)
        verify(reportRepository).findAll(any<Specification<Report>>(), any<Pageable>())
    }

    @Test
    @DisplayName("신고 상세 조회 성공")
    fun t2_getReportDetailSuccess() {
        // given
        whenever(adminRepository.findById(admin.id!!)).thenReturn(Optional.of(admin))
        whenever(reportRepository.findById(testReport.id!!)).thenReturn(Optional.of(testReport))

        // when
        val result = adminReportService.getReportDetail(admin.id!!, testReport.id!!)

        // then
        assertThat(result.reportId).isEqualTo(testReport.id)
        assertThat(result.reporterInfo.email).isEqualTo(reporter.email)
    }

    @Test
    @DisplayName("신고 반려(REJECT) 처리 성공")
    fun t3_processReportActionReject() {
        // given
        val request = AdminReportActionRequest(AdminReportActionRequest.ActionType.REJECT)
        whenever(adminRepository.findById(admin.id!!)).thenReturn(Optional.of(admin))
        whenever(reportRepository.findById(testReport.id!!)).thenReturn(Optional.of(testReport))

        // when
        val result = adminReportService.processReportAction(admin.id!!, testReport.id!!, request)

        // then
        assertThat(result.newReportStatus).isEqualTo("REJECTED")
        assertThat(testReport.reportStatus).isEqualTo(ReportStatus.REJECTED)
    }

    @Test
    @DisplayName("피신고자 강제 탈퇴(DEACTIVATE) 처리 성공")
    fun t4_processReportActionDeactivate() {
        // given
        val request = AdminReportActionRequest(AdminReportActionRequest.ActionType.DEACTIVATE)
        whenever(adminRepository.findById(admin.id!!)).thenReturn(Optional.of(admin))
        whenever(reportRepository.findById(testReport.id!!)).thenReturn(Optional.of(testReport))
        whenever(reportRepository.findByReporterOrReported(reported, reported)).thenReturn(listOf(testReport))

        // when
        val result = adminReportService.processReportAction(admin.id!!, testReport.id!!, request)

        // then
        assertThat(result.newReportStatus).isEqualTo("RESOLVED")
        assertThat(testReport.reportStatus).isEqualTo(ReportStatus.RESOLVED)
        verify(userRepository, times(1)).delete(reported)
        verify(matchRepository, times(1)).deleteAllBySenderOrReceiver(reported, reported)
        verify(userProfileRepository, times(1)).deleteByUserId(reported.id!!)
    }

    @Test
    @DisplayName("관리자가 아닌 경우 예외 발생")
    fun t5_checkIsAdminFail() {
        // given
        whenever(adminRepository.findById(admin.id!!)).thenReturn(Optional.empty())

        // when & then
        val exception = assertThrows<ServiceException> {
            adminReportService.getReports(admin.id!!, PageRequest.of(0, 10), null, null)
        }
        assertThat(exception.message).isEqualTo("관리자 권한이 필요합니다.")
    }
}