package dsmhackathon18.yesandaero.domain.partnership.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class PartnershipAlreadyExistsException(
    message: String = PartnershipErrorCode.PARTNERSHIP_ALREADY_EXISTS.message,
) : BusinessException(PartnershipErrorCode.PARTNERSHIP_ALREADY_EXISTS, message)
