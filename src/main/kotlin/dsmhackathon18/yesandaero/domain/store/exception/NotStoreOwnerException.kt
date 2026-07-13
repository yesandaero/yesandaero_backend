package dsmhackathon18.yesandaero.domain.store.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class NotStoreOwnerException(
    message: String = StoreErrorCode.NOT_STORE_OWNER.message,
) : BusinessException(StoreErrorCode.NOT_STORE_OWNER, message)
