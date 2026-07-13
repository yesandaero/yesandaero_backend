package dsmhackathon18.yesandaero.domain.store.entity

enum class StoreCategory(
    val label: String,
) {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    SNACK("분식"),
    CAFE("카페"),
}
