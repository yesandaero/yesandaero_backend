package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class NotCouponOwnerException(
    message: String = CouponErrorCode.NOT_COUPON_OWNER.message,
) : BusinessException(CouponErrorCode.NOT_COUPON_OWNER, message)
