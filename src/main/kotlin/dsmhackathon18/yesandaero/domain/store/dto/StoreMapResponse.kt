package dsmhackathon18.yesandaero.domain.store.dto

data class StoreMapResponse(
    val stores: List<StoreSummaryResponse>,
    val totalInBounds: Int,
    val truncated: Boolean,
)
