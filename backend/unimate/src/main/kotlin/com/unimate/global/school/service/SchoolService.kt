package com.unimate.global.school.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.unimate.domain.verification.dto.SchoolListResponse
import com.unimate.global.csv.generator.SchoolDomainData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class SchoolService(
    private val objectMapper: ObjectMapper,

    @Value("\${school-data.cache-dir:./cache}")
    private val cacheDir: String,

    @Value("\${school-data.cache-file-name:schools.json}")
    private val cacheFileName: String
) {

    private val log = LoggerFactory.getLogger(SchoolService::class.java)
    private var schoolDomainsCache: List<SchoolDomainData>? = null

    fun getSchoolDomains(schoolName: String): List<String> {
        val schools = loadSchoolDomains()

        val foundSchool = schools.find { it.schoolName == schoolName }

        if (foundSchool != null) {
            log.info("학교 도메인 조회 성공: {} -> {}", schoolName, foundSchool.domains)
            return foundSchool.domains
        }

        log.warn("학교를 찾을 수 없습니다: {}", schoolName)
        return emptyList()
    }

    fun getPrimarySchoolDomain(schoolName: String): String? {
        val domains = getSchoolDomains(schoolName)
        return domains.firstOrNull()
    }

    fun getAllSchools(): List<SchoolListResponse> {
        val schools = loadSchoolDomains()
        return schools.map { school ->
            SchoolListResponse(
                schoolName = school.schoolName,
                domain = school.domains.firstOrNull() ?: ""
            )
        }
    }

    private fun loadSchoolDomains(): List<SchoolDomainData> {
        if (schoolDomainsCache != null) {
            return schoolDomainsCache!!
        }

        try {
            val cacheFile = File(cacheDir, cacheFileName)

            if (!cacheFile.exists()) {
                log.warn("학교 도메인 파일이 없습니다: {}", cacheFile.absolutePath)
                return emptyList()
            }

            val json = cacheFile.readText(Charsets.UTF_8)
            schoolDomainsCache = objectMapper.readValue(json)

            log.info("학교 도메인 로드 완료: {} 개", schoolDomainsCache?.size)
            return schoolDomainsCache!!
        } catch (e: Exception) {
            log.error("학교 도메인 파일 읽기 실패: {}", e.message, e)
            return emptyList()
        }
    }

    fun refreshCache() {
        schoolDomainsCache = null
        log.info("학교 도메인 캐시 초기화")
    }
}