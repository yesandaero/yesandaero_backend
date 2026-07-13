package dsmhackathon18.yesandaero.domain.store.dto

import com.fasterxml.jackson.annotation.JsonFormat
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.global.util.Distance
import java.time.LocalTime

data class StoreDetailResponse(
    val storeId: Long,
    val name: String,
    val category: StoreCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val avgPrice: Int,
    val description: String?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val openTime: LocalTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val closeTime: LocalTime,
    val minOrderAmount: Int,
    val distanceMeters: Long?,
    val walkingMinutes: Int?,
    val menus: List<MenuResponse>,
    // TODO: coupon 도메인 완성 후 실제 보유 쿠폰 수로 대체
    val usableCouponCount: Int,
) {

    companion object {

        fun of(
            store: Store,
            menus: List<MenuResponse>,
            distance: Distance?,
            usableCouponCount: Int = 0,
        ): StoreDetailResponse =
            StoreDetailResponse(
                storeId = requireNotNull(store.id),
                name = store.name,
                category = store.category,
                address = store.address,
                latitude = store.latitude,
                longitude = store.longitude,
                phone = store.phone,
                avgPrice = store.avgPrice,
                description = store.description,
                openTime = store.openTime,
                closeTime = store.closeTime,
                minOrderAmount = store.minOrderAmount,
                distanceMeters = distance?.distanceMeters,
                walkingMinutes = distance?.walkingMinutes,
                menus = menus,
                usableCouponCount = usableCouponCount,
            )
    }
}
