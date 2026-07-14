package dsmhackathon18.yesandaero.domain.coupon.dto

import dsmhackathon18.yesandaero.domain.coupon.entity.CouponTemplate
import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType

data class CouponTemplateResponse(
    val templateId: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val minOrderAmount: Int,
    val validDays: Int,
    val active: Boolean,
) {

    companion object {

        fun of(template: CouponTemplate): CouponTemplateResponse =
            CouponTemplateResponse(
                templateId = requireNotNull(template.id),
                name = template.name,
                discountType = template.discountType,
                discountValue = template.discountValue,
                minOrderAmount = template.minOrderAmount,
                validDays = template.validDays,
                active = template.active,
            )
    }
}
