package dsmhackathon18.yesandaero.global.exception

class ConflictException(
    message: String = GlobalErrorCode.CONFLICT.message,
) : BusinessException(GlobalErrorCode.CONFLICT, message)
