package dsmhackathon18.yesandaero.domain.store.dto

import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory

data class StoreSearchItemResponse(
    val storeId: Long,
    val name: String,
    val category: StoreCategory,
    val partnershipStatus: PartnershipStatus,
)

data class StoreSearchListResponse(
    val content: List<StoreSearchItemResponse>,
    val page: Int,
    val totalPages: Int,
)
