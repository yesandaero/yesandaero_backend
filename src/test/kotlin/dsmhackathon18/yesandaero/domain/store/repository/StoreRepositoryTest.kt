package dsmhackathon18.yesandaero.domain.store.repository

import dsmhackathon18.yesandaero.domain.store.entity.Menu
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.data.domain.PageRequest
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StoreRepositoryTest {

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var menuRepository: MenuRepository

    @AfterEach
    fun cleanUp() {
        menuRepository.deleteAll()
        storeRepository.deleteAll()
    }

    private fun store(
        ownerUserId: Long,
        name: String,
        category: StoreCategory,
        avgPrice: Int,
        latitude: Double = 36.3624,
        longitude: Double = 127.3568,
    ): Store =
        storeRepository.save(
            Store(
                ownerUserId = ownerUserId,
                name = name,
                category = category,
                address = "대전시 유성구",
                phone = null,
                avgPrice = avgPrice,
                description = null,
                latitude = latitude,
                longitude = longitude,
                openTime = LocalTime.of(9, 0),
                closeTime = LocalTime.of(21, 0),
                minOrderAmount = 0,
            ),
        )

    @Test
    fun `카테고리와 최대가격으로 필터링한다`() {
        store(1L, "한식집", StoreCategory.KOREAN, 8000)
        store(2L, "중식집", StoreCategory.CHINESE, 12000)
        store(3L, "저렴한 한식집", StoreCategory.KOREAN, 5000)

        val page = storeRepository.findAllByFilters(
            listOf(StoreCategory.KOREAN),
            8000,
            PageRequest.of(0, 10),
        )

        assertEquals(2, page.totalElements)
        assertTrue(page.content.all { it.category == StoreCategory.KOREAN })
    }

    @Test
    fun `할인율이 큰 가게 순으로 정렬한다 - 할인 메뉴 없는 가게는 맨 뒤`() {
        val noDiscountStore = store(1L, "할인없음", StoreCategory.KOREAN, 8000)
        val bigDiscountStore = store(2L, "큰할인", StoreCategory.KOREAN, 8000)
        val smallDiscountStore = store(3L, "작은할인", StoreCategory.KOREAN, 8000)
        val noMenuStore = store(4L, "메뉴없음", StoreCategory.KOREAN, 8000)

        menuRepository.save(Menu(noDiscountStore, "메뉴A", null, 10000, null, 0))
        menuRepository.save(Menu(bigDiscountStore, "메뉴B", null, 10000, 5000, 0)) // 50% 할인
        menuRepository.save(Menu(smallDiscountStore, "메뉴C", null, 10000, 9000, 0)) // 10% 할인

        val page = storeRepository.findAllByFiltersOrderByDiscountDesc(
            StoreCategory.entries.toList(),
            null,
            PageRequest.of(0, 10),
        )

        val order = page.content.map { it.name }
        assertEquals(4, page.totalElements)
        assertEquals("큰할인", order[0])
        assertEquals("작은할인", order[1])
        assertTrue(order.indexOf("할인없음") > 1)
        assertTrue(order.indexOf("메뉴없음") > 1)
    }

    @Test
    fun `가격 오름차순 정렬용 쿼리는 페이지 정보를 포함한다`() {
        store(1L, "가게A", StoreCategory.KOREAN, 10000)
        store(2L, "가게B", StoreCategory.KOREAN, 5000)
        store(3L, "가게C", StoreCategory.KOREAN, 8000)

        val page = storeRepository.findAllByFilters(
            StoreCategory.entries.toList(),
            null,
            PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("avgPrice").ascending()),
        )

        assertEquals(listOf("가게B", "가게C"), page.content.map { it.name })
        assertEquals(3, page.totalElements)
        assertEquals(2, page.totalPages)
    }

    @Test
    fun `바운딩 박스 안의 가게만 조회한다`() {
        store(1L, "박스안", StoreCategory.KOREAN, 8000, latitude = 36.36, longitude = 127.35)
        store(2L, "박스밖", StoreCategory.KOREAN, 8000, latitude = 37.5, longitude = 127.0)

        val result = storeRepository.findAllInBoundingBox(
            swLat = 36.0,
            swLng = 127.0,
            neLat = 36.5,
            neLng = 127.5,
            categories = StoreCategory.entries.toList(),
            maxPrice = null,
        )

        assertEquals(listOf("박스안"), result.map { it.name })
    }
}
