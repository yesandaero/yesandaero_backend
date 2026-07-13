package dsmhackathon18.yesandaero.global.exception

class InvalidRequestException(
    message: String = GlobalErrorCode.INVALID_REQUEST.message,
) : BusinessException(GlobalErrorCode.INVALID_REQUEST, message)
