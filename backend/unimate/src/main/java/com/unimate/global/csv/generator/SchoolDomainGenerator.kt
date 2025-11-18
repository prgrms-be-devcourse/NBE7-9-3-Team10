package com.unimate.global.csv.generator

import com.fasterxml.jackson.databind.ObjectMapper
import com.unimate.global.csv.provider.SchoolDataProvider
import com.unimate.global.school.service.SchoolService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class SchoolDomainDataGenerator(
    private val schoolDataProvider: SchoolDataProvider,
    private val objectMapper: ObjectMapper,
    private val schoolService: SchoolService,

    @Value("\${school-data.cache-dir:./cache}")
    private val cacheDir: String,

    @Value("\${school-data.cache-file-name:school-domains.json}")
    private val cacheFileName: String
) {

    private val log = LoggerFactory.getLogger(SchoolDomainDataGenerator::class.java)
    private val cacheFile: File
        get() = File(cacheDir, cacheFileName)

    init {
        File(cacheDir).mkdirs()
        log.info("캐시 디렉토리 생성: $cacheDir")
    }

    fun generateNow() {
        log.info("파일 생성 시작...")
        try {
            val startTime = System.currentTimeMillis()

            val schoolDomainList = fetchAndProcessSchoolData()

            if (schoolDomainList.isEmpty()) {
                log.warn("다운로드된 데이터가 없습니다")
                return
            }

            saveToFile(schoolDomainList)
            schoolService.refreshCache()

            val endTime = System.currentTimeMillis()
            log.info("파일 생성 완료 (${(endTime - startTime) / 1000}초)")

        } catch (e: Exception) {
            log.error("파일 생성 실패: ${e.message}", e)
        }
    }

    private fun fetchAndProcessSchoolData(): List<SchoolDomainData> {
        val schoolDomainList = mutableListOf<SchoolDomainData>()
        var pageNo = 1
        val maxPages = 100

        while (pageNo <= maxPages) {
            val (schools, totalCount) = schoolDataProvider.getSchoolsByPage(pageNo)

            if (schools.isEmpty()) {
                log.debug("페이지 $pageNo 데이터 없음 - 종료")
                break
            }

            schools.forEach { school ->
                if (school.schoolName.isNotEmpty() && school.homepageAdres.isNotEmpty()) {
                    val domains = extractDomains(school.homepageAdres)
                    if (domains.isNotEmpty()) {
                        schoolDomainList.add(
                            SchoolDomainData(
                                schoolName = school.schoolName,
                                domains = domains
                            )
                        )
                    }
                }
            }

            if (schoolDomainList.size >= totalCount) {
                log.debug("모든 데이터 다운로드 완료")
                break
            }

            pageNo++
            log.debug("진행: $pageNo 페이지")
        }

        return schoolDomainList.distinctBy { it.schoolName }
    }

    private fun extractDomains(homepageAddress: String): List<String> {
        if (homepageAddress.isEmpty()) return emptyList()

        val items = homepageAddress.split(",")

        return items.mapNotNull { item ->
            var domain = item.trim()
                .removeSuffix("/")
                .lowercase()
                .trim('"')
                .trim('\'')

            val isDomain = domain.contains(".") ||
                    domain.startsWith("http://") ||
                    domain.startsWith("https://")

            if (!isDomain) {
                return@mapNotNull null
            }

            domain = when {
                domain.startsWith("https://") -> domain.substring(8)
                domain.startsWith("http://") -> domain.substring(7)
                else -> domain
            }

            if (domain.startsWith("www.")) {
                domain = domain.substring(4)
            }

            domain = domain.substringBefore(":")
            domain = domain.substringBefore("/")

            if (domain.isNotEmpty() && domain.contains(".")) {
                domain
            } else {
                null
            }
        }.distinct()
    }

    private fun saveToFile(schoolDomainList: List<SchoolDomainData>) {
        try {
            val json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(schoolDomainList)

            cacheFile.writeText(json, Charsets.UTF_8)

            log.info("school-domains.json 저장 완료")
            log.info("경로: ${cacheFile.absolutePath}")
            log.info("학교 수: ${schoolDomainList.size}")
            log.info("파일 크기: ${String.format("%.2f", cacheFile.length() / 1024.0)}KB")

            schoolDomainList.take(3).forEach { school ->
                log.info("예: ${school.schoolName} -> ${school.domains}")
            }
        } catch (e: Exception) {
            log.error("파일 저장 실패: ${e.message}", e)
            throw e
        }
    }

    fun fileExists(): Boolean {
        return cacheFile.exists()
    }

    fun printFileInfo() {
        if (!fileExists()) {
            log.info("school-domains.json 파일이 없습니다")
            return
        }

        log.info("school-domains.json 파일 정보")
        log.info("경로: ${cacheFile.absolutePath}")
        log.info("크기: ${String.format("%.2f", cacheFile.length() / 1024.0)}KB")
    }
}