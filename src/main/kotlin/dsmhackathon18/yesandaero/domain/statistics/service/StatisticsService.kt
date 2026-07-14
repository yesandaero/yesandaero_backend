package dsmhackathon18.yesandaero.domain.statistics.service

import dsmhackathon18.yesandaero.domain.coupon.repository.CouponRepository
import dsmhackathon18.yesandaero.domain.statistics.dto.IssuedStatsResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.RedeemedByIssuerStoreResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.RedeemedStatsResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.StatisticsPeriodResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.StoreStatisticsResponse
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StatisticsService(
    private val storeRepository: StoreRepository,
    private val couponRepository: CouponRepository,
) {

    @Transactional(readOnly = true)
    fun getStoreStatistics(ownerUserId: Long, storeId: Long, from: LocalDate, to: LocalDate): StoreStatisticsResponse {
        val store = storeRepository.findById(storeId).orElseThrow { StoreNotFoundException() }
        if (store.ownerUserId != ownerUserId) throw NotStoreOwnerException()

        val fromDateTime = from.atStartOfDay()
        val toDateTimeExclusive = to.plusDays(1).atStartOfDay()

        val issuedRow = couponRepository.getIssuedStats(storeId, fromDateTime, toDateTimeExclusive)
        val redeemedTotal = couponRepository.getRedeemedTotal(storeId, fromDateTime, toDateTimeExclusive)
        val byIssuerRows = couponRepository.getRedeemedByIssuerStore(storeId, fromDateTime, toDateTimeExclusive)

        val issuerStores = storeRepository.findAllById(byIssuerRows.map { it.storeId }).associateBy { it.id }
        val byIssuerStore = byIssuerRows.mapNotNull { row ->
            val issuerStore = issuerStores[row.storeId] ?: return@mapNotNull null
            RedeemedByIssuerStoreResponse(storeId = row.storeId, name = issuerStore.name, count = row.count.toInt())
        }

        return StoreStatisticsResponse(
            period = StatisticsPeriodResponse(from = from, to = to),
            issued = IssuedStatsResponse(
                total = issuedRow.total.toInt(),
                registered = issuedRow.registered.toInt(),
                used = issuedRow.used.toInt(),
            ),
            redeemedAtMyStore = RedeemedStatsResponse(total = redeemedTotal.toInt(), byIssuerStore = byIssuerStore),
        )
    }
}
