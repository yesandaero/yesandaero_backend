package dsmhackathon18.yesandaero.domain.store.service

import dsmhackathon18.yesandaero.domain.store.dto.MenuBulkUpdateRequest
import dsmhackathon18.yesandaero.domain.store.dto.MenuRequest
import dsmhackathon18.yesandaero.domain.store.dto.StoreListSort
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
import dsmhackathon18.yesandaero.global.exception.InvalidRequestException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun `존재하지 않는 가게의 메뉴를 수정하면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            storeService.replaceMenus(1L, 999L, MenuBulkUpdateRequest(emptyList()))
        }
    }

    @Test
    fun `본인 가게가 아닌 가게의 메뉴를 수정하면 NotStoreOwnerException이 발생한다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)

        assertFailsWith<NotStoreOwnerException> {
            storeService.replaceMenus(2L, 10L, MenuBulkUpdateRequest(emptyList()))
        }

        verify(exactly = 0) { menuRepository.deleteAllByStoreId(any()) }
    }

    @Test
    fun `메뉴 전체 교체 시 기존 메뉴를 삭제하고 요청 순서대로 display_order를 부여해 저장한다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)
        every { menuRepository.deleteAllByStoreId(10L) } returns Unit
        val savedMenus = slot<List<Menu>>()
        every { menuRepository.saveAll(capture(savedMenus)) } answers {
            savedMenus.captured.forEachIndexed { index, menu ->
                ReflectionTestUtils.setField(menu, "id", (index + 1).toLong())
            }
            savedMenus.captured
        }

        val request = MenuBulkUpdateRequest(
            listOf(
                MenuRequest("제육볶음", "매콤한 제육", 9000, 8000),
                MenuRequest("된장찌개", "집된장 사용", 8000, null),
            ),
        )

        val response = storeService.replaceMenus(1L, 10L, request)

        verify { menuRepository.deleteAllByStoreId(10L) }
        assertEquals(2, savedMenus.captured.size)
        assertEquals(0, savedMenus.captured[0].displayOrder)
        assertEquals(1, savedMenus.captured[1].displayOrder)
        assertEquals(2, response.menus.size)
    }

    @Test
    fun `빈 메뉴 목록으로 교체하면 기존 메뉴만 삭제되고 저장은 호출되지 않는다`() {
        val store = existingStore()
        every { storeRepository.findById(10L) } returns Optional.of(store)
        every { menuRepository.deleteAllByStoreId(10L) } returns Unit

        val response = storeService.replaceMenus(1L, 10L, MenuBulkUpdateRequest(emptyList()))

        verify { menuRepository.deleteAllByStoreId(10L) }
        verify(exactly = 0) { menuRepository.saveAll(any<List<Menu>>()) }
        assertEquals(0, response.menus.size)
    }

    @Test
    fun `카테고리 목록을 조회하면 6개 카테고리를 코드와 한글 라벨로 반환한다`() {
        val response = storeService.getCategories()

        assertEquals(6, response.categories.size)
        assertEquals(
            listOf("KOREAN", "CHINESE", "JAPANESE", "WESTERN", "SNACK", "CAFE"),
            response.categories.map { it.code },
        )
        assertEquals("한식", response.categories.first { it.code == "KOREAN" }.label)
    }

    @Test
    fun `maxPrice가 음수이면 InvalidRequestException이 발생한다`() {
        assertFailsWith<InvalidRequestException> {
            storeService.listStores(null, -1000, null, null, null, 0, 20)
        }
    }

    @Test
    fun `DISTANCE_ASC 정렬인데 좌표가 없으면 InvalidRequestException이 발생한다`() {
        assertFailsWith<InvalidRequestException> {
            storeService.listStores(null, null, null, null, StoreListSort.DISTANCE_ASC, 0, 20)
        }
    }

    @Test
    fun `카테고리를 지정하지 않으면 전체 카테고리로 조회한다`() {
        val categoriesSlot = slot<List<StoreCategory>>()
        every {
            storeRepository.findAllByFilters(capture(categoriesSlot), null, any<Pageable>())
        } returns emptyPage()

        storeService.listStores(null, null, null, null, StoreListSort.PRICE_ASC, 0, 20)

        assertEquals(StoreCategory.entries.toSet(), categoriesSlot.captured.toSet())
    }

    @Test
    fun `PRICE_ASC 정렬은 avgPrice 오름차순 페이지를 조회한다`() {
        val pageableSlot = slot<Pageable>()
        every {
            storeRepository.findAllByFilters(listOf(StoreCategory.KOREAN), 10000, capture(pageableSlot))
        } returns emptyPage()

        storeService.listStores(listOf(StoreCategory.KOREAN), 10000, null, null, StoreListSort.PRICE_ASC, 0, 20)

        assertEquals(Sort.by(Sort.Direction.ASC, "avgPrice"), pageableSlot.captured.sort)
    }

    @Test
    fun `DISCOUNT_DESC 정렬은 할인율 정렬 전용 쿼리를 사용한다`() {
        every {
            storeRepository.findAllByFiltersOrderByDiscountDesc(StoreCategory.entries.toList(), null, any())
        } returns emptyPage()

        storeService.listStores(null, null, null, null, StoreListSort.DISCOUNT_DESC, 0, 20)

        verify { storeRepository.findAllByFiltersOrderByDiscountDesc(StoreCategory.entries.toList(), null, any()) }
    }

    @Test
    fun `정렬 조건과 좌표가 모두 없으면 createdAt 내림차순으로 조회한다`() {
        val pageableSlot = slot<Pageable>()
        every {
            storeRepository.findAllByFilters(StoreCategory.entries.toList(), null, capture(pageableSlot))
        } returns emptyPage()

        storeService.listStores(null, null, null, null, null, 0, 20)

        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageableSlot.captured.sort)
    }

    @Test
    fun `좌표만 있고 정렬 조건이 없으면 DISTANCE_ASC로 기본 정렬하고 거리 정보를 포함한다`() {
        val near = existingStore()
        every {
            storeRepository.findAllByFilters(StoreCategory.entries.toList(), null)
        } returns listOf(near)

        val response = storeService.listStores(null, null, 36.3624, 127.3568, null, 0, 20)

        assertEquals(1, response.content.size)
        assertNotNull(response.content[0].distanceMeters)
    }

    @Test
    fun `바운딩 박스가 뒤집혀 있으면 InvalidRequestException이 발생한다`() {
        assertFailsWith<InvalidRequestException> {
            storeService.getStoresInBounds(36.5, 127.5, 36.0, 127.0, null, null, 100, null, null)
        }
    }

    @Test
    fun `바운딩 박스 내 가게 수가 limit을 초과하면 truncated가 true다`() {
        val stores = (1..3).map {
            existingStore()
        }
        every {
            storeRepository.findAllInBoundingBox(36.0, 127.0, 36.5, 127.5, StoreCategory.entries.toList(), null)
        } returns stores

        val response = storeService.getStoresInBounds(36.0, 127.0, 36.5, 127.5, null, null, 2, null, null)

        assertEquals(3, response.totalInBounds)
        assertEquals(2, response.stores.size)
        assertTrue(response.truncated)
    }

    @Test
    fun `바운딩 박스 내 가게 수가 limit 이하이면 truncated가 false다`() {
        every {
            storeRepository.findAllInBoundingBox(36.0, 127.0, 36.5, 127.5, StoreCategory.entries.toList(), null)
        } returns listOf(existingStore())

        val response = storeService.getStoresInBounds(36.0, 127.0, 36.5, 127.5, null, null, 100, null, null)

        assertEquals(1, response.totalInBounds)
        assertEquals(false, response.truncated)
    }

    private fun emptyPage(): Page<Store> = PageImpl(emptyList(), PageRequest.of(0, 20), 0)

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
