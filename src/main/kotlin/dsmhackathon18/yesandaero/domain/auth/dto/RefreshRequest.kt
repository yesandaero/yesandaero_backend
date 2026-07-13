package dsmhackathon18.yesandaero.domain.auth.dto

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank(message = "refreshToken은 필수입니다")
    val refreshToken: String,
)
