package dsmhackathon18.yesandaero.global.exception

class ForbiddenException(
    message: String = GlobalErrorCode.FORBIDDEN.message,
) : BusinessException(GlobalErrorCode.FORBIDDEN, message)
