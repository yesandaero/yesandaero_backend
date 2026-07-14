package dsmhackathon18.yesandaero.domain.coupon.dto

import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType
import java.time.LocalDateTime

data class CouponUseResponse(
    val couponId: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val status: CouponStatus,
    val usedAt: LocalDateTime,
)
