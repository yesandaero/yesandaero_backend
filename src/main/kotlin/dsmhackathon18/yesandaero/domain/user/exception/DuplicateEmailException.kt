package dsmhackathon18.yesandaero.domain.user.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class DuplicateEmailException(
    message: String = UserErrorCode.DUPLICATE_EMAIL.message,
) : BusinessException(UserErrorCode.DUPLICATE_EMAIL, message)
