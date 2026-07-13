package dsmhackathon18.yesandaero.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "email은 필수입니다")
    @field:Email(message = "email 형식이 올바르지 않습니다")
    val email: String,

    @field:NotBlank(message = "password는 필수입니다")
    val password: String,
)
