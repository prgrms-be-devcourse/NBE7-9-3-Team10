package com.unimate.global.exception

import lombok.Getter
import org.springframework.http.HttpStatus

//서비스에서 발생하는 예외 처리용 공통 예외 클래스
@Getter
class ServiceException(
    val status: HttpStatus,
    val errorCode: String,
    message: String
) : RuntimeException(message) {
    companion object {
        @JvmStatic
        fun badRequest(message: String): ServiceException {
            return ServiceException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message)
        }

        @JvmStatic
        fun unauthorized(message: String): ServiceException {
            return ServiceException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message)
        }

        @JvmStatic
        fun notFound(message: String): ServiceException {
            return ServiceException(HttpStatus.NOT_FOUND, "NOT_FOUND", message)
        }

        @JvmStatic
        fun conflict(message: String): ServiceException {
            return ServiceException(HttpStatus.CONFLICT, "CONFLICT", message)
        }

        @JvmStatic
        fun forbidden(message: String): ServiceException {
            return ServiceException(HttpStatus.FORBIDDEN, "FORBIDDEN", message)
        }

        @JvmStatic
        fun internalServerError(message: String): ServiceException {
            return ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message)
        }
    }
}

