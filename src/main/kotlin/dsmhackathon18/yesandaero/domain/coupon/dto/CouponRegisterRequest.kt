package dsmhackathon18.yesandaero.domain.coupon.dto

import jakarta.validation.constraints.NotBlank

data class CouponRegisterRequest(
    @field:NotBlank(message = "token은 필수입니다")
    val token: String,
)
