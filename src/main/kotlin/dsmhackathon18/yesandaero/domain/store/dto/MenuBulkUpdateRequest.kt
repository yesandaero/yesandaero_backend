package dsmhackathon18.yesandaero.domain.store.dto

import jakarta.validation.Valid

data class MenuBulkUpdateRequest(
    @field:Valid
    val menus: List<MenuRequest> = emptyList(),
)
