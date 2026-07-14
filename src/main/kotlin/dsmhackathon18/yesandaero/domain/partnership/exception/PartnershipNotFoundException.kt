package dsmhackathon18.yesandaero.domain.partnership.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class PartnershipNotFoundException(
    message: String = PartnershipErrorCode.PARTNERSHIP_NOT_FOUND.message,
) : BusinessException(PartnershipErrorCode.PARTNERSHIP_NOT_FOUND, message)
