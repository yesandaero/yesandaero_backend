package dsmhackathon18.yesandaero.domain.store.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class StoreNotFoundException(
    message: String = StoreErrorCode.STORE_NOT_FOUND.message,
) : BusinessException(StoreErrorCode.STORE_NOT_FOUND, message)
