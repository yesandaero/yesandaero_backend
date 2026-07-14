package dsmhackathon18.yesandaero.global.exception.handler

import dsmhackathon18.yesandaero.domain.user.exception.UserErrorCode
import dsmhackathon18.yesandaero.global.exception.BusinessException
import dsmhackathon18.yesandaero.global.exception.ErrorResponse
import dsmhackathon18.yesandaero.global.exception.GlobalErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errorCode = e.errorCode
        val message = e.message ?: errorCode.message

        if (errorCode.status.is5xxServerError) {
            log.error("Business exception: {} {}", request.method, request.requestURI, e)
        } else {
            log.warn("Business exception: {} {} - {}", request.method, request.requestURI, message)
        }

        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode.status, errorCode.code, message, request.requestURI))
    }

    // @Valid 바인딩 실패
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.joinToString(", ") { fe ->
            "${fe.field}: ${fe.defaultMessage ?: "invalid"}"
        }
        log.warn("Validation exception: {} {} - {}", request.method, request.requestURI, message)

        val errorCode = GlobalErrorCode.INVALID_REQUEST
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode.status, errorCode.code, message, request.requestURI))
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
        HttpRequestMethodNotSupportedException::class,
        HttpMediaTypeNotSupportedException::class,
    )
    fun handleBadRequest(e: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("Bad request: {} {} - {}", request.method, request.requestURI, e.message)

        val errorCode = GlobalErrorCode.INVALID_REQUEST
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode, request.requestURI))
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(e: AuthenticationException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("Authentication exception: {} {} - {}", request.method, request.requestURI, e.message)

        val errorCode = GlobalErrorCode.UNAUTHORIZED
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode, request.requestURI))
    }

    // @PreAuthorize("hasRole(...)") 등 role 기반 인가 실패
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("Access denied: {} {} - {}", request.method, request.requestURI, e.message)

        val errorCode = UserErrorCode.ROLE_NOT_ALLOWED
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode, request.requestURI))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        // 서버 로그에만 스택 트레이스 남기고, 응답은 안전하게
        log.error("Unexpected exception: {} {}", request.method, request.requestURI, e)

        val errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode, request.requestURI))
    }
}
