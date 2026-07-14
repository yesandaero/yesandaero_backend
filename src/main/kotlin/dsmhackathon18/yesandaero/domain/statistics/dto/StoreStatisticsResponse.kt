package dsmhackathon18.yesandaero.domain.statistics.dto

import java.time.LocalDate

data class StoreStatisticsResponse(
    val period: StatisticsPeriodResponse,
    val issued: IssuedStatsResponse,
    val redeemedAtMyStore: RedeemedStatsResponse,
)

data class StatisticsPeriodResponse(
    val from: LocalDate,
    val to: LocalDate,
)

data class IssuedStatsResponse(
    val total: Int,
    val registered: Int,
    val used: Int,
)

data class RedeemedStatsResponse(
    val total: Int,
    val byIssuerStore: List<RedeemedByIssuerStoreResponse>,
)

data class RedeemedByIssuerStoreResponse(
    val storeId: Long,
    val name: String,
    val count: Int,
)
