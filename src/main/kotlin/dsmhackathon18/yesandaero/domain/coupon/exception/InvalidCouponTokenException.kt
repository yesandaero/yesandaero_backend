package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class InvalidCouponTokenException(
    message: String = CouponErrorCode.INVALID_COUPON_TOKEN.message,
) : BusinessException(CouponErrorCode.INVALID_COUPON_TOKEN, message)
