package dsmhackathon18.yesandaero.global.exception

import org.springframework.http.HttpStatus

enum class GlobalErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {

    // 400
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "GLB_400", "잘못된 요청입니다."),

    // 401/403
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLB_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "GLB_403", "권한이 없습니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "GLB_404", "리소스를 찾을 수 없습니다."),

    // 409
    CONFLICT(HttpStatus.CONFLICT, "GLB_409", "요청이 충돌했습니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLB_500", "서버 오류가 발생했습니다."),
}
