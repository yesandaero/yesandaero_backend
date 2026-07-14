package dsmhackathon18.yesandaero.domain.store.dto

import com.fasterxml.jackson.annotation.JsonFormat
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.global.util.Distance
import java.time.LocalTime

data class StoreSummaryResponse(
    val storeId: Long,
    val name: String,
    val category: StoreCategory,
    val avgPrice: Int,
    val latitude: Double,
    val longitude: Double,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val openTime: LocalTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val closeTime: LocalTime,
    val minOrderAmount: Int,
    val distanceMeters: Long?,
    val walkingMinutes: Int?,
    // TODO: coupon 도메인 완성 후 실제 사용 가능 쿠폰 보유 여부로 대체
    val hasUsableCoupon: Boolean,
) {

    companion object {

        fun of(store: Store, distance: Distance?, hasUsableCoupon: Boolean = false): StoreSummaryResponse =
            StoreSummaryResponse(
                storeId = requireNotNull(store.id),
                name = store.name,
                category = store.category,
                avgPrice = store.avgPrice,
                latitude = store.latitude,
                longitude = store.longitude,
                openTime = store.openTime,
                closeTime = store.closeTime,
                minOrderAmount = store.minOrderAmount,
                distanceMeters = distance?.distanceMeters,
                walkingMinutes = distance?.walkingMinutes,
                hasUsableCoupon = hasUsableCoupon,
            )
    }
}
