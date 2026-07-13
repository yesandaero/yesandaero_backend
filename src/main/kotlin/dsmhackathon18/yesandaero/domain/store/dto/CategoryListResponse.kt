package dsmhackathon18.yesandaero.domain.store.dto

import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory

data class CategoryItem(
    val code: String,
    val label: String,
)

data class CategoryListResponse(
    val categories: List<CategoryItem>,
) {

    companion object {

        fun all(): CategoryListResponse =
            CategoryListResponse(
                categories = StoreCategory.entries.map { CategoryItem(code = it.name, label = it.label) },
            )
    }
}
