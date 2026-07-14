package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class TemplateAccessNotAllowedException(
    message: String = CouponErrorCode.TEMPLATE_ACCESS_NOT_ALLOWED.message,
) : BusinessException(CouponErrorCode.TEMPLATE_ACCESS_NOT_ALLOWED, message)
