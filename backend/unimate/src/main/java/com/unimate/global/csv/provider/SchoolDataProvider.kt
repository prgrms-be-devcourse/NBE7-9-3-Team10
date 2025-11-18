package com.unimate.global.csv.provider

import com.unimate.global.csv.dto.SchoolDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class SchoolDataProvider {

    private val log = LoggerFactory.getLogger(SchoolDataProvider::class.java)

    fun loadAllSchools(): List<SchoolDto> {
        val schools = mutableListOf<SchoolDto>()

        try {
            val inputStream = this.javaClass.classLoader.getResourceAsStream("data/schools.csv")
                ?: throw Exception("schools.csv 파일을 찾을 수 없습니다 (src/main/resources/data/schools.csv 확인)")

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine() // 헤더 읽기

                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(",", limit = 2)

                    if (parts.size >= 2) {
                        val schoolName = parts[0].trim().trim('"')
                        val homepageAddress = parts[1].trim().trim('"')

                        if (schoolName.isNotEmpty() && homepageAddress.isNotEmpty()) {
                            schools.add(
                                SchoolDto(
                                    schoolName = schoolName,
                                    homepageAdres = homepageAddress
                                )
                            )
                        }
                    }
                }

                log.info("CSV 파일 로드 완료: ${schools.size}개 학교")
            }
        } catch (e: Exception) {
            log.error("CSV 파일 읽기 실패: ${e.message}", e)
            throw e
        }

        return schools
    }

    fun getSchoolsByPage(pageNo: Int = 1, pageSize: Int = 100): Pair<List<SchoolDto>, Int> {
        val allSchools = loadAllSchools()
        val totalCount = allSchools.size
        val startIndex = (pageNo - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, totalCount)

        val pagedSchools = if (startIndex < totalCount) {
            allSchools.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        log.info("페이지 조회: ${pagedSchools.size}개 학교, 총 ${totalCount}개 (페이지 $pageNo)")

        return Pair(pagedSchools, totalCount)
    }
}