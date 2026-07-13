package dsmhackathon18.yesandaero.global.exception

class UnauthorizedException(
    message: String = GlobalErrorCode.UNAUTHORIZED.message,
) : BusinessException(GlobalErrorCode.UNAUTHORIZED, message)
