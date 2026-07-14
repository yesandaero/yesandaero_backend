package dsmhackathon18.yesandaero.domain.store.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class StoreAlreadyExistsException(
    message: String = StoreErrorCode.STORE_ALREADY_EXISTS.message,
) : BusinessException(StoreErrorCode.STORE_ALREADY_EXISTS, message)
