package dsmhackathon18.yesandaero.domain.user.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class LoginFailedException(
    message: String = UserErrorCode.LOGIN_FAILED.message,
) : BusinessException(UserErrorCode.LOGIN_FAILED, message)
