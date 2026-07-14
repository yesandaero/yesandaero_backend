package dsmhackathon18.yesandaero.domain.statistics.service

import dsmhackathon18.yesandaero.domain.coupon.repository.CouponRepository
import dsmhackathon18.yesandaero.domain.coupon.repository.IssuedStatsRow
import dsmhackathon18.yesandaero.domain.coupon.repository.IssuerStoreCountRow
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StatisticsServiceTest {

    private val storeRepository = mockk<StoreRepository>()
    private val couponRepository = mockk<CouponRepository>()
    private val statisticsService = StatisticsService(storeRepository, couponRepository)

    private fun store(id: Long, ownerUserId: Long, name: String = "가게$id"): Store =
        Store(
            ownerUserId = ownerUserId,
            name = name,
            category = StoreCategory.KOREAN,
            address = "대전시 유성구",
            phone = null,
            avgPrice = 8000,
            description = null,
            latitude = 36.3624,
            longitude = 127.3568,
            openTime = LocalTime.of(9, 0),
            closeTime = LocalTime.of(21, 0),
            minOrderAmount = 0,
        ).also { ReflectionTestUtils.setField(it, "id", id) }

    @Test
    fun `가게가 없으면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findById(1L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            statisticsService.getStoreStatistics(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 13))
        }
    }

    @Test
    fun `본인 가게가 아니면 NotStoreOwnerException이 발생한다`() {
        every { storeRepository.findById(1L) } returns Optional.of(store(1L, ownerUserId = 999L))

        assertFailsWith<NotStoreOwnerException> {
            statisticsService.getStoreStatistics(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 13))
        }
    }

    @Test
    fun `발급-사용 통계와 발급 가게별 사용 집계를 반환한다`() {
        val ownerUserId = 1L
        val storeId = 10L
        val from = LocalDate.of(2026, 7, 1)
        val to = LocalDate.of(2026, 7, 13)
        val fromDateTime = from.atStartOfDay()
        val toDateTimeExclusive = to.plusDays(1).atStartOfDay()

        every { storeRepository.findById(storeId) } returns Optional.of(store(storeId, ownerUserId))
        every { couponRepository.getIssuedStats(storeId, fromDateTime, toDateTimeExclusive) } returns
            IssuedStatsRow(total = 120, registered = 80, used = 45)
        every { couponRepository.getRedeemedTotal(storeId, fromDateTime, toDateTimeExclusive) } returns 30L
        every { couponRepository.getRedeemedByIssuerStore(storeId, fromDateTime, toDateTimeExclusive) } returns
            listOf(IssuerStoreCountRow(storeId = 12L, count = 18L), IssuerStoreCountRow(storeId = 15L, count = 12L))
        every { storeRepository.findAllById(listOf(12L, 15L)) } returns
            listOf(store(12L, ownerUserId = 2L, name = "흔카페"), store(15L, ownerUserId = 3L, name = "유성분식"))

        val response = statisticsService.getStoreStatistics(ownerUserId, storeId, from, to)

        assertEquals(from, response.period.from)
        assertEquals(to, response.period.to)
        assertEquals(120, response.issued.total)
        assertEquals(80, response.issued.registered)
        assertEquals(45, response.issued.used)
        assertEquals(30, response.redeemedAtMyStore.total)
        assertEquals(2, response.redeemedAtMyStore.byIssuerStore.size)
        assertEquals(12L, response.redeemedAtMyStore.byIssuerStore[0].storeId)
        assertEquals("흔카페", response.redeemedAtMyStore.byIssuerStore[0].name)
        assertEquals(18, response.redeemedAtMyStore.byIssuerStore[0].count)
    }

    @Test
    fun `발급 가게 정보가 이미 삭제된 경우 해당 항목은 결과에서 제외한다`() {
        val ownerUserId = 1L
        val storeId = 10L
        val from = LocalDate.of(2026, 7, 1)
        val to = LocalDate.of(2026, 7, 13)
        val fromDateTime = from.atStartOfDay()
        val toDateTimeExclusive = to.plusDays(1).atStartOfDay()

        every { storeRepository.findById(storeId) } returns Optional.of(store(storeId, ownerUserId))
        every { couponRepository.getIssuedStats(storeId, fromDateTime, toDateTimeExclusive) } returns
            IssuedStatsRow(total = 0, registered = 0, used = 0)
        every { couponRepository.getRedeemedTotal(storeId, fromDateTime, toDateTimeExclusive) } returns 5L
        every { couponRepository.getRedeemedByIssuerStore(storeId, fromDateTime, toDateTimeExclusive) } returns
            listOf(IssuerStoreCountRow(storeId = 99L, count = 5L))
        every { storeRepository.findAllById(listOf(99L)) } returns emptyList()

        val response = statisticsService.getStoreStatistics(ownerUserId, storeId, from, to)

        assertEquals(5, response.redeemedAtMyStore.total)
        assertEquals(emptyList(), response.redeemedAtMyStore.byIssuerStore)
    }
}
