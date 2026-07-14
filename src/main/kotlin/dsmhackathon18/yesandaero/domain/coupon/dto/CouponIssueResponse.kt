package dsmhackathon18.yesandaero.domain.coupon.dto

data class CouponIssueResponse(
    val couponId: Long,
    val qrPayload: String,
    val expiresIn: Int,
)
