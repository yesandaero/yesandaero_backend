package dsmhackathon18.yesandaero.domain.coupon.repository

import dsmhackathon18.yesandaero.domain.coupon.entity.Coupon
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CouponRepositoryTest {

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    // 로컬 개발 DB를 공유하는 환경에서 실제 데이터와 충돌하지 않도록 큰 값에서 시작한다.
    private val idBase = 900_000_000L + (System.nanoTime() % 1_000_000L)

    @AfterEach
    fun cleanUp() {
        couponRepository.deleteAll()
    }

    private fun coupon(
        status: CouponStatus = CouponStatus.ISSUED,
        userId: Long? = null,
        expiresAt: LocalDateTime? = null,
        issuerStoreId: Long = idBase + 2,
        targetStoreId: Long = idBase + 3,
        usedStoreId: Long? = null,
        issuedAt: LocalDateTime? = null,
        registeredAt: LocalDateTime? = null,
        usedAt: LocalDateTime? = null,
    ): Coupon {
        val entity = Coupon(
            templateId = idBase + 1,
            issuerStoreId = issuerStoreId,
            targetStoreId = targetStoreId,
            status = status,
        )
        if (userId != null) {
            ReflectionTestUtils.setField(entity, "userId", userId)
        }
        if (expiresAt != null) {
            ReflectionTestUtils.setField(entity, "expiresAt", expiresAt)
        }
        if (usedStoreId != null) {
            ReflectionTestUtils.setField(entity, "usedStoreId", usedStoreId)
        }
        if (issuedAt != null) {
            ReflectionTestUtils.setField(entity, "issuedAt", issuedAt)
        }
        if (registeredAt != null) {
            ReflectionTestUtils.setField(entity, "registeredAt", registeredAt)
        }
        if (usedAt != null) {
            ReflectionTestUtils.setField(entity, "usedAt", usedAt)
        }
        return couponRepository.save(entity)
    }

    @Test
    fun `REGISTERED 상태의 쿠폰은 사용 처리되고 두 번째 시도는 실패한다`() {
        val registered = coupon(status = CouponStatus.REGISTERED, userId = idBase + 4)
        val couponId = requireNotNull(registered.id)

        val firstResult = couponRepository.useIfRegistered(couponId, LocalDateTime.now())
        val secondResult = couponRepository.useIfRegistered(couponId, LocalDateTime.now())

        assertEquals(1, firstResult)
        assertEquals(0, secondResult)

        // 벌크 UPDATE는 1차 캐시를 갱신하지 않으므로, 실제로 재조회를 검증하려면 초기화가 필요하다.
        entityManager.clear()
        val used = couponRepository.findById(couponId).orElseThrow()
        assertEquals(CouponStatus.USED, used.status)
        assertEquals(used.targetStoreId, used.usedStoreId)
    }

    @Test
    fun `ISSUED 상태의 쿠폰은 사용 처리되지 않는다`() {
        val issued = coupon(status = CouponStatus.ISSUED)

        val result = couponRepository.useIfRegistered(requireNotNull(issued.id), LocalDateTime.now())

        assertEquals(0, result)
    }

    @Test
    fun `만료 지난 REGISTERED 쿠폰은 일괄적으로 EXPIRED로 전이된다`() {
        val userId = idBase + 5
        val expired = coupon(status = CouponStatus.REGISTERED, userId = userId, expiresAt = LocalDateTime.now().minusDays(1))
        val stillValid = coupon(status = CouponStatus.REGISTERED, userId = userId, expiresAt = LocalDateTime.now().plusDays(1))

        val affected = couponRepository.expireOverdueCoupons(userId, LocalDateTime.now())

        assertEquals(1, affected)
        entityManager.clear()
        assertEquals(CouponStatus.EXPIRED, couponRepository.findById(requireNotNull(expired.id)).orElseThrow().status)
        assertEquals(CouponStatus.REGISTERED, couponRepository.findById(requireNotNull(stillValid.id)).orElseThrow().status)
    }

    @Test
    fun `사용 가능한 쿠폰 개수와 대상 가게 목록을 올바르게 조회한다`() {
        val userId = idBase + 6
        coupon(status = CouponStatus.REGISTERED, userId = userId, expiresAt = LocalDateTime.now().plusDays(1))

        val count = couponRepository.countUsable(userId, idBase + 3, LocalDateTime.now())
        val storeIds = couponRepository.findTargetStoreIdsWithUsableCoupon(userId, listOf(idBase + 3, idBase + 999), LocalDateTime.now())

        assertEquals(1, count)
        assertTrue(storeIds.contains(idBase + 3))
        assertEquals(1, storeIds.size)
    }

    @Test
    fun `getIssuedStats는 기간 내 발급 총합-등록-사용 건수를 집계한다`() {
        val issuerStoreId = idBase + 10
        val from = LocalDateTime.of(2026, 7, 1, 0, 0)
        val to = LocalDateTime.of(2026, 7, 14, 0, 0)

        coupon(status = CouponStatus.ISSUED, issuerStoreId = issuerStoreId, issuedAt = from.plusDays(1))
        coupon(
            status = CouponStatus.REGISTERED,
            issuerStoreId = issuerStoreId,
            issuedAt = from.plusDays(2),
            registeredAt = from.plusDays(3),
        )
        coupon(
            status = CouponStatus.USED,
            issuerStoreId = issuerStoreId,
            issuedAt = from.plusDays(4),
            registeredAt = from.plusDays(5),
            usedAt = from.plusDays(6),
        )
        // 기간 밖 발급 - 집계에서 제외되어야 한다
        coupon(status = CouponStatus.ISSUED, issuerStoreId = issuerStoreId, issuedAt = to.plusDays(1))

        val result = couponRepository.getIssuedStats(issuerStoreId, from, to)

        assertEquals(3, result.total)
        assertEquals(2, result.registered)
        assertEquals(1, result.used)
    }

    @Test
    fun `getRedeemedTotal과 getRedeemedByIssuerStore는 사용 매장 기준으로 발급 가게별 사용 건수를 내림차순 집계한다`() {
        val myStoreId = idBase + 20
        val issuerA = idBase + 21
        val issuerB = idBase + 22
        val from = LocalDateTime.of(2026, 7, 1, 0, 0)
        val to = LocalDateTime.of(2026, 7, 14, 0, 0)

        coupon(status = CouponStatus.USED, issuerStoreId = issuerA, targetStoreId = myStoreId, usedStoreId = myStoreId, usedAt = from.plusDays(1))
        coupon(status = CouponStatus.USED, issuerStoreId = issuerA, targetStoreId = myStoreId, usedStoreId = myStoreId, usedAt = from.plusDays(2))
        coupon(status = CouponStatus.USED, issuerStoreId = issuerB, targetStoreId = myStoreId, usedStoreId = myStoreId, usedAt = from.plusDays(3))
        // 기간 밖 사용 - 제외
        coupon(status = CouponStatus.USED, issuerStoreId = issuerA, targetStoreId = myStoreId, usedStoreId = myStoreId, usedAt = to.plusDays(1))
        // 아직 사용되지 않음 - 제외
        coupon(status = CouponStatus.REGISTERED, issuerStoreId = issuerA, targetStoreId = myStoreId)

        val total = couponRepository.getRedeemedTotal(myStoreId, from, to)
        val byIssuerStore = couponRepository.getRedeemedByIssuerStore(myStoreId, from, to)

        assertEquals(3, total)
        assertEquals(listOf(issuerA to 2L, issuerB to 1L), byIssuerStore.map { it.storeId to it.count })
    }
}
