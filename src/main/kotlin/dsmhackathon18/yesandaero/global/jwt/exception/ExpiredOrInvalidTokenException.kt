package dsmhackathon18.yesandaero.global.jwt.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class ExpiredOrInvalidTokenException(
    message: String = JwtErrorCode.EXPIRED_OR_INVALID_TOKEN.message,
) : BusinessException(JwtErrorCode.EXPIRED_OR_INVALID_TOKEN, message)
