package dsmhackathon18.yesandaero.domain.partnership.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class InvalidPartnershipStatusException(
    message: String = PartnershipErrorCode.INVALID_PARTNERSHIP_STATUS.message,
) : BusinessException(PartnershipErrorCode.INVALID_PARTNERSHIP_STATUS, message)
