package dsmhackathon18.yesandaero.domain.auth.dto

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
)
