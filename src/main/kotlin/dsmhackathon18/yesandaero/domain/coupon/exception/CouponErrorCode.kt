package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CouponErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {

    // 401
    INVALID_COUPON_TOKEN(HttpStatus.UNAUTHORIZED, "CPN_401", "쿠폰 토큰이 만료되었거나 유효하지 않습니다"),

    // 403
    ISSUE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "CPN_403_01", "쿠폰 발급 권한이 없습니다"),
    NOT_COUPON_OWNER(HttpStatus.FORBIDDEN, "CPN_403_02", "본인의 쿠폰이 아닙니다"),

    // 404
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CPN_404_01", "존재하지 않는 쿠폰 템플릿입니다"),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CPN_404_02", "존재하지 않는 쿠폰입니다"),

    // 409
    COUPON_ALREADY_REGISTERED(HttpStatus.CONFLICT, "CPN_409_01", "이미 등록된 쿠폰입니다"),
    INVALID_COUPON_STATUS(HttpStatus.CONFLICT, "CPN_409_02", "사용할 수 없는 상태의 쿠폰입니다"),
}
