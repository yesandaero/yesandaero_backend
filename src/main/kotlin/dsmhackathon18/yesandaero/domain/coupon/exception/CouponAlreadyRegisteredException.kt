package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class CouponAlreadyRegisteredException(
    message: String = CouponErrorCode.COUPON_ALREADY_REGISTERED.message,
) : BusinessException(CouponErrorCode.COUPON_ALREADY_REGISTERED, message)
