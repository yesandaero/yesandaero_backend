package dsmhackathon18.yesandaero.domain.store.service

import dsmhackathon18.yesandaero.domain.store.dto.MenuRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreUpdateRequest
import dsmhackathon18.yesandaero.domain.store.entity.Menu
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreAlreadyExistsException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.MenuRepository
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StoreServiceTest {

    private val storeRepository = mockk<StoreRepository>()
    private val menuRepository = mockk<MenuRepository>()
    private val storeService = StoreService(storeRepository, menuRepository)

    private fun registerRequest(menus: List<MenuRequest> = emptyList()) = StoreRegisterRequest(
        name = "시흔식당",
        category = StoreCategory.KOREAN,
        address = "대전시 유성구",
        latitude = 36.3624,
        longitude = 127.3568,
        phone = "042-000-0000",
        avgPrice = 9000,
        description = "백반 전문점",
        openTime = LocalTime.of(9, 0),
        closeTime = LocalTime.of(21, 0),
        minOrderAmount = 8000,
        menus = menus,
    )

    @Test
    fun `이미 가게를 등록한 owner가 다시 등록하면 StoreAlreadyExistsException이 발생한다`() {
        every { storeRepository.existsByOwnerUserId(1L) } returns true

        assertFailsWith<StoreAlreadyExistsException> {
            storeService.registerStore(1L, registerRequest())
        }

        verify(exactly = 0) { storeRepository.save(any()) }
    }

    @Test
    fun `가게 등록에 성공하면 storeId를 반환한다`() {
        every { storeRepository.existsByOwnerUserId(1L) } returns false
        val savedStore = slot<Store>()
        every { storeRepository.save(capture(savedStore)) } answers {
            ReflectionTestUtils.setField(savedStore.captured, "id", 10L)
            savedStore.captured
        }

        val response = storeService.registerStore(1L, registerRequest())

        assertEquals(10L, response.storeId)
        assertEquals(1L, savedStore.captured.ownerUserId)
        verify(exactly = 0) { menuRepository.saveAll(any<List<Menu>>()) }
    }

    @Test
    fun `가게 등록 시 메뉴 목록을 display_order 순서로 함께 저장한다`() {
        every { storeRepository.existsByOwnerUserId(1L) } returns false
        val savedStore = slot<Store>()
        every { storeRepository.save(capture(savedStore)) } answers {
            ReflectionTestUtils.setField(savedStore.captured, "id", 10L)
            savedStore.captured
        }
        val savedMenus = slot<List<Menu>>()
        every { menuRepository.saveAll(capture(savedMenus)) } answers { savedMenus.captured }

        val request = registerRequest(
            menus = listOf(
                MenuRequest("제육볶음", "매콤한 제육", 9000, 8000),
                MenuRequest("된장찌개", null, 8000, null),
            ),
        )

        storeService.registerStore(1L, request)

        assertEquals(2, savedMenus.captured.size)
        assertEquals(0, savedMenus.captured[0].displayOrder)
        assertEquals("제육볶음", savedMenus.captured[0].name)
        assertEquals(1, savedMenus.captured[1].displayOrder)
        assertEquals("된장찌개", savedMenus.captured[1].name)
    }

    @Test
    fun `존재하지 않는 가게를 상세 조회하면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            storeService.getStoreDetail(999L, lat = null, lng = null)
        }
    }

    @Test
    fun `좌표 없이 가게 상세를 조회하면 거리 정보는 null이다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)
        every { menuRepository.findByStoreIdOrderByDisplayOrderAsc(10L) } returns emptyList()

        val response = storeService.getStoreDetail(10L, lat = null, lng = null)

        assertNull(response.distanceMeters)
        assertNull(response.walkingMinutes)
        assertEquals(0, response.usableCouponCount)
    }

    @Test
    fun `좌표와 함께 가게 상세를 조회하면 거리 정보가 포함된다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)
        every { menuRepository.findByStoreIdOrderByDisplayOrderAsc(10L) } returns emptyList()

        val response = storeService.getStoreDetail(10L, lat = 36.3624, lng = 127.3568)

        assertNotNull(response.distanceMeters)
        assertNotNull(response.walkingMinutes)
    }

    @Test
    fun `등록된 가게가 없는 owner가 내 가게를 조회하면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns null

        assertFailsWith<StoreNotFoundException> {
            storeService.getMyStore(1L)
        }
    }

    @Test
    fun `내 가게를 조회하면 거리 정보 없이 상세 정보를 반환한다`() {
        val store = existingStore()
        every { storeRepository.findByOwnerUserId(1L) } returns store
        every { menuRepository.findByStoreIdOrderByDisplayOrderAsc(10L) } returns emptyList()

        val response = storeService.getMyStore(1L)

        assertEquals(10L, response.storeId)
        assertNull(response.distanceMeters)
    }

    @Test
    fun `존재하지 않는 가게를 수정하면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            storeService.updateStore(1L, 999L, updateRequest())
        }
    }

    @Test
    fun `본인 가게가 아닌 가게를 수정하면 NotStoreOwnerException이 발생한다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)

        assertFailsWith<NotStoreOwnerException> {
            storeService.updateStore(2L, 10L, updateRequest())
        }
    }

    @Test
    fun `가게 정보 수정에 성공하면 변경된 필드가 반영된 상세 정보를 반환한다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)
        every { menuRepository.findByStoreIdOrderByDisplayOrderAsc(10L) } returns emptyList()

        val response = storeService.updateStore(
            1L,
            10L,
            updateRequest(avgPrice = 10000, description = "백반, 찌개 전문점", minOrderAmount = 9000),
        )

        assertEquals(10000, response.avgPrice)
        assertEquals("백반, 찌개 전문점", response.description)
        assertEquals(9000, response.minOrderAmount)
        assertEquals("시흔식당", response.name)
    }

    private fun updateRequest(
        avgPrice: Int? = null,
        description: String? = null,
        minOrderAmount: Int? = null,
    ) = StoreUpdateRequest(
        name = null,
        category = null,
        address = null,
        latitude = null,
        longitude = null,
        phone = null,
        avgPrice = avgPrice,
        description = description,
        openTime = null,
        closeTime = null,
        minOrderAmount = minOrderAmount,
    )

    private fun existingStore(): Store =
        Store(
            ownerUserId = 1L,
            name = "시흔식당",
            category = StoreCategory.KOREAN,
            address = "대전시 유성구",
            phone = "042-000-0000",
            avgPrice = 9000,
            description = "백반 전문점",
            latitude = 36.3624,
            longitude = 127.3568,
            openTime = LocalTime.of(9, 0),
            closeTime = LocalTime.of(21, 0),
            minOrderAmount = 8000,
        ).also { ReflectionTestUtils.setField(it, "id", 10L) }
}
