package dsmhackathon18.yesandaero.domain.coupon.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class IssueNotAllowedException(
    message: String = CouponErrorCode.ISSUE_NOT_ALLOWED.message,
) : BusinessException(CouponErrorCode.ISSUE_NOT_ALLOWED, message)
