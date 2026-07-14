package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class CouponNotFoundException(
    message: String = CouponErrorCode.COUPON_NOT_FOUND.message,
) : BusinessException(CouponErrorCode.COUPON_NOT_FOUND, message)
