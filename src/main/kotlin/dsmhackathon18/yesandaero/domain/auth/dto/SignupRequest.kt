package dsmhackathon18.yesandaero.domain.auth.dto

import dsmhackathon18.yesandaero.domain.user.entity.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:NotBlank(message = "username은 필수입니다")
    @field:Size(max = 50, message = "username은 50자를 초과할 수 없습니다")
    val username: String,

    @field:NotBlank(message = "email은 필수입니다")
    @field:Email(message = "email 형식이 올바르지 않습니다")
    @field:Size(max = 50, message = "email은 50자를 초과할 수 없습니다")
    val email: String,

    @field:NotBlank(message = "password는 필수입니다")
    val password: String,

    @field:NotNull(message = "role은 필수입니다")
    val role: Role,
)
