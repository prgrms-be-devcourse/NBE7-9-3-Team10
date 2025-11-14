package com.unimate.domain.report.repository

import com.unimate.domain.report.entity.Report
import com.unimate.domain.report.entity.ReportStatus
import com.unimate.domain.user.user.entity.User
import jakarta.persistence.criteria.Join
import org.springframework.data.jpa.domain.Specification

object ReportSpecification {
    fun withStatus(status: ReportStatus): Specification<Report> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get<ReportStatus>("reportStatus"), status)
        }
    }

    fun withKeyword(keyword: String): Specification<Report> {
        return Specification { root, _, criteriaBuilder ->
            val pattern = "%$keyword%"

            val reporterJoin: Join<Report, User> = root.join("reporter")
            val reportedJoin: Join<Report, User> = root.join("reported")

            criteriaBuilder.or(
                criteriaBuilder.like(reporterJoin.get("name"), pattern),
                criteriaBuilder.like(reportedJoin.get("name"), pattern),
                criteriaBuilder.like(root.get("content"), pattern)
            )
        }
    }
}