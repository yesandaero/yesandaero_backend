package dsmhackathon18.yesandaero.domain.coupon.dto

import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import java.time.LocalDateTime

data class CouponRegisterResponse(
    val couponId: Long,
    val name: String,
    val store: CouponStoreResponse,
    val status: CouponStatus,
    val expiresAt: LocalDateTime,
)
