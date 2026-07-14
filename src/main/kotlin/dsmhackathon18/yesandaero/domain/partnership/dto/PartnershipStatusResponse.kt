package dsmhackathon18.yesandaero.domain.partnership.dto

import com.fasterxml.jackson.annotation.JsonInclude
import dsmhackathon18.yesandaero.domain.partnership.entity.Partnership
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PartnershipStatusResponse(
    val partnershipId: Long,
    val status: PartnershipStatus,
    val acceptedAt: LocalDateTime?,
) {

    companion object {

        fun of(partnership: Partnership): PartnershipStatusResponse =
            PartnershipStatusResponse(
                partnershipId = requireNotNull(partnership.id),
                status = partnership.status,
                acceptedAt = partnership.acceptedAt,
            )
    }
}
