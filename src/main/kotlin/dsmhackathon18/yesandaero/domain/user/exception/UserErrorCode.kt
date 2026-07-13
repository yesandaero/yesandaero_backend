package dsmhackathon18.yesandaero.domain.user.exception

import dsmhackathon18.yesandaero.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {

    // 401
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USR_401", "이메일 또는 비밀번호가 일치하지 않습니다"),

    // 403
    ROLE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "USR_403", "해당 역할로는 수행할 수 없는 요청입니다"),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USR_409", "이미 가입된 이메일입니다"),
}
