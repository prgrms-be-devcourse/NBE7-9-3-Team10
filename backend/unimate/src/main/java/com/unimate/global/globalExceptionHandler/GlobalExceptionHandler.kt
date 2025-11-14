package com.unimate.global.globalExceptionHandler

import com.unimate.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: String = LocalDateTime.now().toString(),
    val status: Int,
    val error: String,
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ResponseEntity<ErrorResponse> {
        log.warn("[ServiceException] {} - {}", ex.errorCode, ex.message)

        val response = ErrorResponse(
            status = ex.status.value(),
            error = ex.errorCode,
            message = ex.message ?: "메세지가 없습니다."
        )

        return ResponseEntity.status(ex.status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult
            .fieldErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "입력값이 올바르지 않습니다."

        log.warn("[ValidationException] {}", errorMessage)

        val response = ErrorResponse(
            status = 400,
            error = "BAD_REQUEST",
            message = errorMessage
        )

        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: {}", ex.message, ex)

        val response = ErrorResponse(
            status = 500,
            error = "INTERNAL_SERVER_ERROR",
            message = ex.message ?: "알 수 없는 오류가 발생했습니다."
        )

        return ResponseEntity.internalServerError().body(response)
    }
}