package dsmhackathon18.yesandaero.global.exception

class EntityNotFoundException(
    message: String = GlobalErrorCode.NOT_FOUND.message,
) : BusinessException(GlobalErrorCode.NOT_FOUND, message)
