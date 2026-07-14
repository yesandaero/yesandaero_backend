package dsmhackathon18.yesandaero.domain.coupon.dto

import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class CouponTemplateCreateRequest(
    @field:NotBlank(message = "name은 필수입니다")
    @field:Size(max = 100, message = "name은 100자를 초과할 수 없습니다")
    val name: String,

    @field:NotNull(message = "discountType은 필수입니다")
    val discountType: DiscountType,

    @field:Positive(message = "discountValue는 0보다 커야 합니다")
    val discountValue: Int,

    @field:PositiveOrZero(message = "minOrderAmount는 0 이상이어야 합니다")
    val minOrderAmount: Int,

    @field:Positive(message = "validDays는 0보다 커야 합니다")
    val validDays: Int,
) {

    @AssertTrue(message = "RATE 할인은 discountValue가 1~100이어야 합니다")
    fun isDiscountValueValid(): Boolean =
        discountType != DiscountType.RATE || discountValue in 1..100
}
