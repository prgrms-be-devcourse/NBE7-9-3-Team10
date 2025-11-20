package com.unimate.domain.report.repository

import com.unimate.domain.report.entity.Report
import com.unimate.domain.user.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface ReportRepository : JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    fun findByReporterOrReported(reporter: User, reported: User) : List<Report>
}