package dsmhackathon18.yesandaero.domain.coupon.service

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateCreateRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateUpdateRequest
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponTemplate
import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType
import dsmhackathon18.yesandaero.domain.coupon.exception.TemplateAccessNotAllowedException
import dsmhackathon18.yesandaero.domain.coupon.exception.TemplateNotFoundException
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponTemplateRepository
import dsmhackathon18.yesandaero.domain.partnership.repository.PartnershipRepository
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CouponTemplateServiceTest {

    private val couponTemplateRepository = mockk<CouponTemplateRepository>()
    private val storeRepository = mockk<StoreRepository>()
    private val partnershipRepository = mockk<PartnershipRepository>()
    private val couponTemplateService =
        CouponTemplateService(couponTemplateRepository, storeRepository, partnershipRepository)

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

    private fun createRequest() = CouponTemplateCreateRequest(
        name = "아메리카노 1000원 할인",
        discountType = DiscountType.AMOUNT,
        discountValue = 1000,
        minOrderAmount = 5000,
        validDays = 14,
    )

    @Test
    fun `내 가게가 없으면 템플릿 생성 시 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns null

        assertFailsWith<StoreNotFoundException> {
            couponTemplateService.createTemplate(1L, createRequest())
        }
    }

    @Test
    fun `템플릿 생성에 성공하면 내 가게 소속으로 저장한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        val savedSlot = slot<CouponTemplate>()
        every { couponTemplateRepository.save(capture(savedSlot)) } answers {
            ReflectionTestUtils.setField(savedSlot.captured, "id", 100L)
            savedSlot.captured
        }

        val response = couponTemplateService.createTemplate(1L, createRequest())

        assertEquals(100L, response.templateId)
        assertEquals(10L, savedSlot.captured.storeId)
        assertTrue(savedSlot.captured.active)
    }

    @Test
    fun `존재하지 않는 템플릿을 수정하면 TemplateNotFoundException이 발생한다`() {
        every { couponTemplateRepository.findById(1L) } returns Optional.empty()

        assertFailsWith<TemplateNotFoundException> {
            couponTemplateService.updateTemplate(1L, 1L, CouponTemplateUpdateRequest(active = false))
        }
    }

    @Test
    fun `템플릿 소유 가게가 아니면 수정 시 NotStoreOwnerException이 발생한다`() {
        val template = existingTemplate(storeId = 10L)
        every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
        every { storeRepository.findById(10L) } returns Optional.of(store(10L, 999L))

        assertFailsWith<NotStoreOwnerException> {
            couponTemplateService.updateTemplate(1L, 1L, CouponTemplateUpdateRequest(active = false))
        }
    }

    @Test
    fun `템플릿 비활성화에 성공하면 active가 false로 바뀐다`() {
        val template = existingTemplate(storeId = 10L)
        every { couponTemplateRepository.findById(1L) } returns Optional.of(template)
        every { storeRepository.findById(10L) } returns Optional.of(store(10L, 1L))

        val response = couponTemplateService.updateTemplate(1L, 1L, CouponTemplateUpdateRequest(active = false))

        assertEquals(false, response.active)
    }

    @Test
    fun `내 가게가 없으면 템플릿 목록 조회 시 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns null

        assertFailsWith<StoreNotFoundException> {
            couponTemplateService.listTemplates(1L, null, null)
        }
    }

    @Test
    fun `active 필터 없이 조회하면 내 가게의 전체 템플릿을 반환한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findAllByStoreId(10L) } returns listOf(existingTemplate(10L))

        val response = couponTemplateService.listTemplates(1L, null, null)

        assertEquals(1, response.templates.size)
    }

    @Test
    fun `active 필터로 조회하면 해당 조건의 템플릿만 반환한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findAllByStoreIdAndActive(10L, true) } returns listOf(existingTemplate(10L))

        val response = couponTemplateService.listTemplates(1L, null, true)

        assertEquals(1, response.templates.size)
    }

    @Test
    fun `ownerStoreId가 내 가게 id면 내 가게 템플릿을 반환한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { couponTemplateRepository.findAllByStoreId(10L) } returns listOf(existingTemplate(10L))

        val response = couponTemplateService.listTemplates(1L, 10L, null)

        assertEquals(1, response.templates.size)
    }

    @Test
    fun `ownerStoreId 가게가 없으면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            couponTemplateService.listTemplates(1L, 999L, null)
        }
    }

    @Test
    fun `ownerStoreId 가게와 제휴 상태가 아니면 TemplateAccessNotAllowedException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns false

        assertFailsWith<TemplateAccessNotAllowedException> {
            couponTemplateService.listTemplates(1L, 20L, null)
        }
    }

    @Test
    fun `ownerStoreId가 제휴 가게면 활성 템플릿만 반환한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.existsAcceptedBetween(10L, 20L) } returns true
        every { couponTemplateRepository.findAllByStoreIdAndActive(20L, true) } returns listOf(existingTemplate(20L))

        val response = couponTemplateService.listTemplates(1L, 20L, false)

        assertEquals(1, response.templates.size)
    }

    private fun existingTemplate(storeId: Long): CouponTemplate =
        CouponTemplate(
            storeId = storeId,
            name = "아메리카노 1000원 할인",
            discountType = DiscountType.AMOUNT,
            discountValue = 1000,
            minOrderAmount = 5000,
            validDays = 14,
            active = true,
        ).also { ReflectionTestUtils.setField(it, "id", 1L) }
}
