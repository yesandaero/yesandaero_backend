package dsmhackathon18.yesandaero.domain.user.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class RoleNotAllowedException(
    message: String = UserErrorCode.ROLE_NOT_ALLOWED.message,
) : BusinessException(UserErrorCode.ROLE_NOT_ALLOWED, message)
