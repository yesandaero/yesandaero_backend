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
    ): Coupon {
        val entity = Coupon(
            templateId = idBase + 1,
            issuerStoreId = idBase + 2,
            targetStoreId = idBase + 3,
            status = status,
        )
        if (userId != null) {
            ReflectionTestUtils.setField(entity, "userId", userId)
        }
        if (expiresAt != null) {
            ReflectionTestUtils.setField(entity, "expiresAt", expiresAt)
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
}
