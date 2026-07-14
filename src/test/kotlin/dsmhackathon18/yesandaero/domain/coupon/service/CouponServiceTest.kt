package dsmhackathon18.yesandaero.domain.coupon.service

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterRequest
import dsmhackathon18.yesandaero.domain.coupon.entity.Coupon
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponTemplate
import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType
import dsmhackathon18.yesandaero.domain.coupon.exception.CouponAlreadyRegisteredException
import dsmhackathon18.yesandaero.domain.coupon.exception.CouponNotFoundException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponStatusException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponTokenException
import dsmhackathon18.yesandaero.domain.coupon.exception.IssueNotAllowedException
import dsmhackathon18.yesandaero.domain.coupon.exception.NotCouponOwnerException
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponIssueTokenRepository
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponRepository
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponTemplateRepository
import dsmhackathon18.yesandaero.domain.partnership.repository.PartnershipRepository
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CouponServiceTest {

    private val couponRepository = mockk<CouponRepository>()
    private val couponTemplateRepository = mockk<CouponTemplateRepository>()
    private val couponIssueTokenRepository = mockk<CouponIssueTokenRepository>()
    private val storeRepository = mockk<StoreRepository>()
    private val partnershipRepository = mockk<PartnershipRepository>()
    private val couponService = CouponService(
        couponRepository,
        couponTemplateRepository,
        couponIssueTokenRepository,
        storeRepository,
        partnershipRepository,
    )

    private fun store(id: Long, ownerUserId: Long): Store =
        Store(
            ownerUserId = ownerUserId,
            name = "가게$id",
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

    private fun template(id: Long, storeId: Long, active: Boolean = true): CouponTemplate =
        CouponTemplate(
            storeId = storeId,
            name = "아메리카노 1000원 할인",
            discountType = DiscountType.AMOUNT,
            discountValue = 1000,
            minOrderAmount = 5000,
            validDays = 14,
            active = active,
        ).also { ReflectionTestUtils.setField(it, "id", id) }

    // ===== issueCoupon =====

    @Test
    fun `내 가게가 없으면 쿠폰 발급 시 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns null

        assertFailsWith<StoreNotFoundException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))
        }
    }

    @Test
    fun `내 템플릿도 아니고 제휴 가게 템플릿도 아니면 쿠폰 발급 시 IssueNotAllowedException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 999L))
        every { partnershipRepository.existsAcceptedBetween(10L, 999L) } returns false

        assertFailsWith<IssueNotAllowedException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))
        }
    }

    @Test
    fun `비활성 템플릿이면 쿠폰 발급 시 IssueNotAllowedException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 10L, active = false))

        assertFailsWith<IssueNotAllowedException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))
        }
    }

    @Test
    fun `대상 가게가 없으면 쿠폰 발급 시 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 10L))
        every { storeRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 999L))
        }
    }

    @Test
    fun `사용처가 제휴 상태가 아니면 쿠폰 발급 시 IssueNotAllowedException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 10L))
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns false

        assertFailsWith<IssueNotAllowedException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))
        }
    }

    @Test
    fun `쿠폰 발급에 성공하면 ISSUED 쿠폰을 저장하고 토큰을 Redis에 저장한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 10L))
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns true
        val savedSlot = slot<Coupon>()
        every { couponRepository.save(capture(savedSlot)) } answers {
            ReflectionTestUtils.setField(savedSlot.captured, "id", 101L)
            savedSlot.captured
        }
        val tokenSlot = slot<String>()
        every {
            couponIssueTokenRepository.save(capture(tokenSlot), 101L, Duration.ofSeconds(600))
        } returns Unit

        val response = couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))

        assertEquals(101L, response.couponId)
        assertEquals(600, response.expiresIn)
        assertEquals("couponapp://register?token=${tokenSlot.captured}", response.qrPayload)
        assertEquals(CouponStatus.ISSUED, savedSlot.captured.status)
        assertEquals(10L, savedSlot.captured.issuerStoreId)
        assertEquals(20L, savedSlot.captured.targetStoreId)
        verify { couponIssueTokenRepository.save(tokenSlot.captured, 101L, Duration.ofSeconds(600)) }
    }

    @Test
    fun `제휴 가게의 활성 템플릿이면 그 가게를 사용처로 발급에 성공한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 20L))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns true
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        val savedSlot = slot<Coupon>()
        every { couponRepository.save(capture(savedSlot)) } answers {
            ReflectionTestUtils.setField(savedSlot.captured, "id", 102L)
            savedSlot.captured
        }
        every { couponIssueTokenRepository.save(any(), 102L, Duration.ofSeconds(600)) } returns Unit

        val response = couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))

        assertEquals(102L, response.couponId)
        assertEquals(10L, savedSlot.captured.issuerStoreId)
        assertEquals(20L, savedSlot.captured.targetStoreId)
    }

    @Test
    fun `제휴 가게의 비활성 템플릿이면 쿠폰 발급 시 IssueNotAllowedException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 20L, active = false))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns true

        assertFailsWith<IssueNotAllowedException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 20L))
        }
    }

    @Test
    fun `제휴 가게 템플릿의 사용처가 템플릿 소유 가게와 다르면 IssueNotAllowedException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 20L))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns true
        every { storeRepository.findById(30L) } returns Optional.of(store(30L, 3L))

        assertFailsWith<IssueNotAllowedException> {
            couponService.issueCoupon(1L, CouponIssueRequest(templateId = 3L, targetStoreId = 30L))
        }
    }

    // ===== registerCoupon =====

    @Test
    fun `토큰이 유효하지 않으면 등록 시 InvalidCouponTokenException이 발생한다`() {
        every { couponIssueTokenRepository.consume("bad-token") } returns null

        assertFailsWith<InvalidCouponTokenException> {
            couponService.registerCoupon(1L, CouponRegisterRequest(token = "bad-token"))
        }
    }

    @Test
    fun `토큰은 유효하지만 쿠폰이 없으면 InvalidCouponTokenException이 발생한다`() {
        every { couponIssueTokenRepository.consume("token") } returns 101L
        every { couponRepository.findById(101L) } returns Optional.empty()

        assertFailsWith<InvalidCouponTokenException> {
            couponService.registerCoupon(1L, CouponRegisterRequest(token = "token"))
        }
    }

    @Test
    fun `쿠폰이 ISSUED 상태가 아니면 CouponAlreadyRegisteredException이 발생한다`() {
        val coupon = existingCoupon(status = CouponStatus.REGISTERED)
        every { couponIssueTokenRepository.consume("token") } returns requireNotNull(coupon.id)
        every { couponRepository.findById(requireNotNull(coupon.id)) } returns Optional.of(coupon)

        assertFailsWith<CouponAlreadyRegisteredException> {
            couponService.registerCoupon(1L, CouponRegisterRequest(token = "token"))
        }
    }

    @Test
    fun `쿠폰 등록에 성공하면 REGISTERED 상태와 만료일을 반환한다`() {
        val coupon = existingCoupon(status = CouponStatus.ISSUED)
        every { couponIssueTokenRepository.consume("token") } returns requireNotNull(coupon.id)
        every { couponRepository.findById(requireNotNull(coupon.id)) } returns Optional.of(coupon)
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 10L))
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))

        val response = couponService.registerCoupon(5L, CouponRegisterRequest(token = "token"))

        assertEquals(CouponStatus.REGISTERED, response.status)
        assertEquals(20L, response.store.storeId)
        assertEquals("아메리카노 1000원 할인", response.name)
    }

    // ===== useCoupon =====

    @Test
    fun `존재하지 않는 쿠폰을 사용하면 CouponNotFoundException이 발생한다`() {
        every { couponRepository.findById(101L) } returns Optional.empty()

        assertFailsWith<CouponNotFoundException> {
            couponService.useCoupon(5L, 101L)
        }
    }

    @Test
    fun `본인 쿠폰이 아니면 사용 시 NotCouponOwnerException이 발생한다`() {
        val coupon = existingCoupon(CouponStatus.REGISTERED, userId = 5L, expiresAt = LocalDateTime.now().plusDays(1))
        every { couponRepository.findById(101L) } returns Optional.of(coupon)

        assertFailsWith<NotCouponOwnerException> {
            couponService.useCoupon(999L, 101L)
        }
    }

    @Test
    fun `REGISTERED 상태가 아니면 사용 시 InvalidCouponStatusException이 발생한다`() {
        val coupon = existingCoupon(CouponStatus.USED, userId = 5L, expiresAt = LocalDateTime.now().plusDays(1))
        every { couponRepository.findById(101L) } returns Optional.of(coupon)

        assertFailsWith<InvalidCouponStatusException> {
            couponService.useCoupon(5L, 101L)
        }

        verify(exactly = 0) { couponRepository.useIfRegistered(any(), any()) }
    }

    @Test
    fun `만료 시점이 지난 쿠폰은 사용 시 InvalidCouponStatusException이 발생한다`() {
        val coupon = existingCoupon(CouponStatus.REGISTERED, userId = 5L, expiresAt = LocalDateTime.now().minusDays(1))
        every { couponRepository.findById(101L) } returns Optional.of(coupon)

        assertFailsWith<InvalidCouponStatusException> {
            couponService.useCoupon(5L, 101L)
        }

        verify(exactly = 0) { couponRepository.useIfRegistered(any(), any()) }
    }

    @Test
    fun `동시 요청으로 이미 처리되어 있으면(원자적 업데이트 0건) InvalidCouponStatusException이 발생한다`() {
        val coupon = existingCoupon(CouponStatus.REGISTERED, userId = 5L, expiresAt = LocalDateTime.now().plusDays(1))
        every { couponRepository.findById(101L) } returns Optional.of(coupon)
        every { couponRepository.useIfRegistered(101L, any()) } returns 0

        assertFailsWith<InvalidCouponStatusException> {
            couponService.useCoupon(5L, 101L)
        }
    }

    @Test
    fun `쿠폰 사용에 성공하면 USED 상태와 할인 정보를 반환한다`() {
        val coupon = existingCoupon(CouponStatus.REGISTERED, userId = 5L, expiresAt = LocalDateTime.now().plusDays(1))
        every { couponRepository.findById(101L) } returns Optional.of(coupon)
        every { couponRepository.useIfRegistered(101L, any()) } returns 1
        every { couponTemplateRepository.findById(3L) } returns Optional.of(template(3L, storeId = 10L))

        val response = couponService.useCoupon(5L, 101L)

        assertEquals(CouponStatus.USED, response.status)
        assertEquals(1000, response.discountValue)
        assertEquals(DiscountType.AMOUNT, response.discountType)
    }

    // ===== listMyCoupons =====

    @Test
    fun `내 쿠폰함 조회 시 만료 지난 쿠폰을 먼저 EXPIRED로 전이한다`() {
        every { couponRepository.expireOverdueCoupons(5L, any()) } returns 1
        every { couponRepository.findAllByUserId(5L) } returns emptyList()

        couponService.listMyCoupons(5L, null)

        verify { couponRepository.expireOverdueCoupons(5L, any()) }
    }

    @Test
    fun `status 필터 없이 조회하면 내 쿠폰 전체를 반환한다`() {
        every { couponRepository.expireOverdueCoupons(5L, any()) } returns 0
        val coupon = existingCoupon(CouponStatus.REGISTERED, userId = 5L, expiresAt = LocalDateTime.now().plusDays(1))
        every { couponRepository.findAllByUserId(5L) } returns listOf(coupon)
        every { storeRepository.findAllById(listOf(20L)) } returns listOf(store(20L, 2L))
        every { couponTemplateRepository.findAllById(listOf(3L)) } returns listOf(template(3L, storeId = 10L))

        val response = couponService.listMyCoupons(5L, null)

        assertEquals(1, response.coupons.size)
        assertEquals(20L, response.coupons[0].store.storeId)
        assertEquals("아메리카노 1000원 할인", response.coupons[0].name)
    }

    @Test
    fun `status 필터로 조회하면 해당 상태의 쿠폰만 조회한다`() {
        every { couponRepository.expireOverdueCoupons(5L, any()) } returns 0
        every { couponRepository.findAllByUserIdAndStatus(5L, CouponStatus.USED) } returns emptyList()

        val response = couponService.listMyCoupons(5L, CouponStatus.USED)

        assertEquals(0, response.coupons.size)
        verify { couponRepository.findAllByUserIdAndStatus(5L, CouponStatus.USED) }
    }

    @Test
    fun `가게 또는 템플릿 정보를 찾을 수 없는 쿠폰은 목록에서 제외한다`() {
        every { couponRepository.expireOverdueCoupons(5L, any()) } returns 0
        val coupon = existingCoupon(CouponStatus.REGISTERED, userId = 5L, expiresAt = LocalDateTime.now().plusDays(1))
        every { couponRepository.findAllByUserId(5L) } returns listOf(coupon)
        every { storeRepository.findAllById(listOf(20L)) } returns emptyList()
        every { couponTemplateRepository.findAllById(listOf(3L)) } returns listOf(template(3L, storeId = 10L))

        val response = couponService.listMyCoupons(5L, null)

        assertEquals(0, response.coupons.size)
    }

    private fun existingCoupon(
        status: CouponStatus,
        userId: Long? = null,
        expiresAt: LocalDateTime? = null,
    ): Coupon =
        Coupon(
            templateId = 3L,
            issuerStoreId = 10L,
            targetStoreId = 20L,
            status = status,
        ).also {
            ReflectionTestUtils.setField(it, "id", 101L)
            if (userId != null) ReflectionTestUtils.setField(it, "userId", userId)
            if (expiresAt != null) ReflectionTestUtils.setField(it, "expiresAt", expiresAt)
        }
}
