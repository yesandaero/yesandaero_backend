package dsmhackathon18.yesandaero.domain.store.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class MenuRequest(
    @field:NotBlank(message = "name은 필수입니다")
    @field:Size(max = 100, message = "name은 100자를 초과할 수 없습니다")
    val name: String,

    @field:Size(max = 500, message = "description은 500자를 초과할 수 없습니다")
    val description: String?,

    @field:Positive(message = "price는 0보다 커야 합니다")
    val price: Int,

    @field:PositiveOrZero(message = "discountedPrice는 0 이상이어야 합니다")
    val discountedPrice: Int?,
) {

    @AssertTrue(message = "discountedPrice는 price보다 작아야 합니다")
    fun isDiscountValid(): Boolean = discountedPrice == null || discountedPrice < price
}
