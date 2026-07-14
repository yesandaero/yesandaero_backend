package dsmhackathon18.yesandaero.domain.partnership.dto

import jakarta.validation.constraints.NotNull

data class PartnershipCreateRequest(
    @field:NotNull(message = "receiverStoreId는 필수입니다")
    val receiverStoreId: Long,
)
