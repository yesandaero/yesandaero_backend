package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class InvalidCouponStatusException(
    message: String = CouponErrorCode.INVALID_COUPON_STATUS.message,
) : BusinessException(CouponErrorCode.INVALID_COUPON_STATUS, message)
