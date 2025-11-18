package com.unimate.domain.report.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.domain.report.dto.AdminReportActionRequest
import com.unimate.domain.report.entity.Report
import com.unimate.domain.report.entity.ReportStatus
import com.unimate.domain.report.repository.ReportRepository
import com.unimate.domain.user.admin.dto.AdminLoginRequest
import com.unimate.domain.user.admin.entity.AdminUser
import com.unimate.domain.user.admin.repository.AdminRepository
import com.unimate.domain.user.user.entity.Gender
import com.unimate.domain.user.user.entity.User
import com.unimate.domain.user.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mail.MailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import com.unimate.global.mail.EmailService
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @Autowired
    private lateinit var adminRepository: AdminRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var reportRepository: ReportRepository

    @MockitoBean
    private lateinit var mailSender: MailSender

    @MockitoBean
    private lateinit var emailService: EmailService

    private lateinit var admin: AdminUser
    private lateinit var reporter: User
    private lateinit var reported: User
    private lateinit var testReport: Report
    private lateinit var adminToken: String

    private val baseUrl = "/api/v1/admin/reports"

    @BeforeEach
    fun setUp() {
        reportRepository.deleteAll()
        userRepository.deleteAll()
        adminRepository.deleteAll()

        admin = createAdmin("admin@test.com", "Admin User")
        reporter = createUser("reporter@test.ac.kr", "Reporter User", Gender.MALE)
        reported = createUser("reported@test.ac.kr", "Reported User", Gender.FEMALE)

        testReport = createReport(reporter, reported)

        adminToken = loginAdmin(admin.email, "password123!")
    }

    private fun createAdmin(email: String, name: String): AdminUser {
        val adminUser = AdminUser(
            email = email,
            password = passwordEncoder.encode("password123!"),
            name = name
        )
        return adminRepository.save(adminUser)
    }

    private fun createUser(email: String, name: String, gender: Gender): User {
        val user = User(
            name = name,
            email = email,
            password = passwordEncoder.encode("password123!"),
            gender = gender,
            birthDate = LocalDate.now().minusYears(25),
            university = "Test University"
        )
        user.studentVerified = true
        return userRepository.save(user)
    }

    private fun createReport(reporter: User, reported: User): Report {
        val report = Report(
            reporter = reporter,
            reported = reported,
            category = "SPAM",
            content = "This is a test report.",
            reportStatus = ReportStatus.RECEIVED
        )
        return reportRepository.save(report)
    }

    private fun loginAdmin(email: String, password: String): String {
        val loginRequest = AdminLoginRequest(email, password)
        val result = mockMvc.perform(
            post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val json = result.response.contentAsString
        return objectMapper.readTree(json).path("accessToken").asText()
    }

    private fun bearer(token: String) = "Bearer $token"

    @Test
    @DisplayName("신고 목록 조회 성공")
    fun getReports_success() {
        testReport.id?.let {
            mockMvc.perform(
                get(baseUrl)
                    .header("Authorization", bearer(adminToken))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reportId").value(it))
        }
    }

    @Test
    @DisplayName("신고 상세 조회 성공")
    fun getReportDetail_success() {
        testReport.id?.let {
            mockMvc.perform(
                get("$baseUrl/$it")
                    .header("Authorization", bearer(adminToken))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.reportId").value(it))
                .andExpect(jsonPath("$.reporterInfo.email").value(reporter.email))
                .andExpect(jsonPath("$.reportedInfo.email").value(reported.email))
        }
    }

    @Test
    @DisplayName("신고 처리 (REJECT) 성공")
    fun handleReportAction_reject_success() {
        testReport.id?.let { reportId ->
            val request = AdminReportActionRequest(AdminReportActionRequest.ActionType.REJECT)

            mockMvc.perform(
                patch("$baseUrl/$reportId/action")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.newReportStatus").value("REJECTED"))

            val updatedReport = reportRepository.findById(reportId).orElseThrow()
            assertThat(updatedReport.reportStatus).isEqualTo(ReportStatus.REJECTED)
        }
    }

    @Test
    @DisplayName("신고 처리 (DEACTIVATE) 성공 - 피신고자 계정 삭제")
    fun handleReportAction_deactivate_success() {
        testReport.id?.let { reportId ->
            val request = AdminReportActionRequest(AdminReportActionRequest.ActionType.DEACTIVATE)

            mockMvc.perform(
                patch("$baseUrl/$reportId/action")
                    .header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.newReportStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.message").value("신고 대상자 계정이 강제 탈퇴 처리되었습니다."))

            val updatedReport = reportRepository.findById(reportId).orElseThrow()
            assertThat(updatedReport.reportStatus).isEqualTo(ReportStatus.RESOLVED)
            reported.id?.let {
                assertThat(userRepository.findById(it)).isEmpty
            }
        }
    }

    @Test
    @DisplayName("인증 없이 API 접근 시 401 Unauthorized 반환")
    fun unauthorizedAccess_shouldFail() {
        mockMvc.perform(get(baseUrl))
            .andExpect(status().isUnauthorized)
    }
}
