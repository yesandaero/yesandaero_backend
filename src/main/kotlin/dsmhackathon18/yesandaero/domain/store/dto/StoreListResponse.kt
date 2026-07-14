package dsmhackathon18.yesandaero.domain.store.dto

data class StoreListResponse(
    val content: List<StoreSummaryResponse>,
    val page: Int,
    val totalPages: Int,
)
