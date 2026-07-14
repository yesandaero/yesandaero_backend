package dsmhackathon18.yesandaero.domain.coupon.dto

import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import java.time.LocalDateTime

data class CouponWalletStoreResponse(
    val storeId: Long,
    val name: String,
    val category: StoreCategory,
)

data class CouponWalletItemResponse(
    val couponId: Long,
    val name: String,
    val store: CouponWalletStoreResponse,
    val status: CouponStatus,
    val expiresAt: LocalDateTime,
)

data class CouponWalletResponse(
    val coupons: List<CouponWalletItemResponse>,
)
