package com.unimate.global.ws

import com.unimate.domain.message.dto.WsError
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@Controller // 필요에 따라 @ControllerAdvice 사용 가능
class StompExceptionAdvice {

    @MessageExceptionHandler(ResponseStatusException::class)
    @SendToUser("/queue/chat.errors")
    fun handleRse(ex: ResponseStatusException, principal: Principal?): WsError {
        return WsError(
            code = ex.statusCode.toString(),
            message = ex.reason ?: "요청 처리 중 오류가 발생했습니다.",
            detail = ex.javaClass.simpleName
        )
    }

    @MessageExceptionHandler(AccessDeniedException::class)
    @SendToUser("/queue/chat.errors")
    fun handleDenied(ex: AccessDeniedException, principal: Principal?): WsError {
        return WsError(
            code = "FORBIDDEN",
            message = "권한이 없습니다.",
            detail = ex.message
        )
    }

    @MessageExceptionHandler(Exception::class)
    @SendToUser("/queue/chat.errors")
    fun handleAny(ex: Exception, principal: Principal?): WsError {
        return WsError(
            code = "INTERNAL_ERROR",
            message = "알 수 없는 오류가 발생했습니다.",
            detail = ex.javaClass.simpleName
        )
    }
}