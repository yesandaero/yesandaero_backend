package dsmhackathon18.yesandaero.global.jwt.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class MalformedTokenException(
    message: String = JwtErrorCode.MALFORMED_TOKEN.message,
) : BusinessException(JwtErrorCode.MALFORMED_TOKEN, message)
