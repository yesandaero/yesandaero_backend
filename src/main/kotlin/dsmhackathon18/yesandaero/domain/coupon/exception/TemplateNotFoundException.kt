package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class TemplateNotFoundException(
    message: String = CouponErrorCode.TEMPLATE_NOT_FOUND.message,
) : BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND, message)
