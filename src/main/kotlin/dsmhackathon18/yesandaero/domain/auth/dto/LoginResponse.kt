package dsmhackathon18.yesandaero.domain.auth.dto

import dsmhackathon18.yesandaero.domain.user.entity.Role

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val role: Role,
)
