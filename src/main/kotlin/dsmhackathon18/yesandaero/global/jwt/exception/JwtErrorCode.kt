package dsmhackathon18.yesandaero.global.jwt.exception

import dsmhackathon18.yesandaero.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class JwtErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {

    // 400
    MALFORMED_TOKEN(HttpStatus.BAD_REQUEST, "JWT_400", "토큰 형식이 올바르지 않습니다"),

    // 401
    EXPIRED_OR_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401", "토큰이 만료되었거나 유효하지 않습니다"),
}
