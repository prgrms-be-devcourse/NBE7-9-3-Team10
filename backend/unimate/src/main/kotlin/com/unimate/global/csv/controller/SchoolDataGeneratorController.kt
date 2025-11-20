package com.unimate.global.csv.controller

import com.unimate.global.csv.generator.SchoolDomainDataGenerator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/school")
@Tag(name = "SchoolDataGeneratorController", description = "학교 데이터 생성 API (관리자용)")
class SchoolDataGeneratorController(
    private val schoolDomainDataGenerator: SchoolDomainDataGenerator
) {

    @PostMapping("/generate")
    @Operation(summary = "CSV에서 JSON 파일 생성 (관리자용)")
    fun generateSchoolData(): ResponseEntity<Map<String, String>> {
        schoolDomainDataGenerator.generateNow()
        return ResponseEntity.ok(mapOf("message" to "학교 도메인 데이터 생성 완료"))
    }

    @GetMapping("/file-info")
    @Operation(summary = "생성된 JSON 파일 정보 조회")
    fun getFileInfo(): ResponseEntity<Map<String, Any>> {
        val fileExists = schoolDomainDataGenerator.fileExists()
        schoolDomainDataGenerator.printFileInfo()

        return ResponseEntity.ok(mapOf(
            "fileExists" to fileExists,
            "message" to if (fileExists) "파일이 존재합니다" else "파일이 없습니다"
        ))
    }
}