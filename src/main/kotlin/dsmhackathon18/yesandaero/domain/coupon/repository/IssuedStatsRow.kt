package dsmhackathon18.yesandaero.domain.coupon.repository

data class IssuedStatsRow(
    val total: Long,
    val registered: Long,
    val used: Long,
)
