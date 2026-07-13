package dsmhackathon18.yesandaero.domain.store.dto

import dsmhackathon18.yesandaero.domain.store.entity.Menu

data class MenuResponse(
    val menuId: Long,
    val name: String,
    val description: String?,
    val price: Int,
    val discountedPrice: Int?,
) {

    companion object {

        fun from(menu: Menu): MenuResponse =
            MenuResponse(
                menuId = requireNotNull(menu.id),
                name = menu.name,
                description = menu.description,
                price = menu.price,
                discountedPrice = menu.discountedPrice,
            )
    }
}
