package dsmhackathon18.yesandaero.domain.coupon.dto

import jakarta.validation.constraints.NotNull

data class CouponIssueRequest(
    @field:NotNull(message = "templateId는 필수입니다")
    val templateId: Long,

    @field:NotNull(message = "targetStoreId는 필수입니다")
    val targetStoreId: Long,
)
