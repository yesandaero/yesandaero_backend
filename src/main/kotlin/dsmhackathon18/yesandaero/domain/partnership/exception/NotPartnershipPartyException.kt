package dsmhackathon18.yesandaero.domain.partnership.exception

import dsmhackathon18.yesandaero.global.exception.BusinessException

class NotPartnershipPartyException(
    message: String = PartnershipErrorCode.NOT_PARTNERSHIP_PARTY.message,
) : BusinessException(PartnershipErrorCode.NOT_PARTNERSHIP_PARTY, message)
