package dsmhackathon18.yesandaero.domain.partnership.dto

import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import java.time.LocalDateTime

data class PartnerStoreResponse(
    val storeId: Long,
    val name: String,
    val category: StoreCategory,
)

data class PartnershipItemResponse(
    val partnershipId: Long,
    val partnerStore: PartnerStoreResponse,
    val direction: PartnershipDirection,
    val status: PartnershipStatus,
    val createdAt: LocalDateTime,
)

data class PartnershipListResponse(
    val partnerships: List<PartnershipItemResponse>,
)
