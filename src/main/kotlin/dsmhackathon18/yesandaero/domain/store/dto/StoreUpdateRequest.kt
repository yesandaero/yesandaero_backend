package dsmhackathon18.yesandaero.domain.store.dto

import com.fasterxml.jackson.annotation.JsonFormat
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalTime

data class StoreUpdateRequest(
    @field:Size(max = 100, message = "name은 100자를 초과할 수 없습니다")
    val name: String?,

    val category: StoreCategory?,

    @field:Size(max = 255, message = "address는 255자를 초과할 수 없습니다")
    val address: String?,

    @field:DecimalMin(value = "-90.0", message = "latitude는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "latitude는 90 이하여야 합니다")
    val latitude: Double?,

    @field:DecimalMin(value = "-180.0", message = "longitude는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "longitude는 180 이하여야 합니다")
    val longitude: Double?,

    @field:Size(max = 20, message = "phone은 20자를 초과할 수 없습니다")
    val phone: String?,

    @field:Positive(message = "avgPrice는 0보다 커야 합니다")
    val avgPrice: Int?,

    @field:Size(max = 500, message = "description은 500자를 초과할 수 없습니다")
    val description: String?,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val openTime: LocalTime?,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val closeTime: LocalTime?,

    @field:PositiveOrZero(message = "minOrderAmount는 0 이상이어야 합니다")
    val minOrderAmount: Int?,
)
