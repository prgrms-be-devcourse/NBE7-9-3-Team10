package com.unimate.global.exception

import org.springframework.http.HttpStatus

class ServiceException(
    val status: HttpStatus,
    val errorCode: String,
    message: String
) : RuntimeException(message) {
    companion object {

        fun badRequest(message: String): ServiceException =
            ServiceException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message)

        fun unauthorized(message: String): ServiceException =
            ServiceException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message)

        fun notFound(message: String): ServiceException =
            ServiceException(HttpStatus.NOT_FOUND, "NOT_FOUND", message)

        fun conflict(message: String): ServiceException =
            ServiceException(HttpStatus.CONFLICT, "CONFLICT", message)

        fun forbidden(message: String): ServiceException =
            ServiceException(HttpStatus.FORBIDDEN, "FORBIDDEN", message)

        fun internalServerError(message: String): ServiceException =
            ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message)
    }
}