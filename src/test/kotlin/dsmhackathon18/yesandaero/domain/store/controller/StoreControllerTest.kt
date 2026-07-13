package dsmhackathon18.yesandaero.domain.store.controller

import dsmhackathon18.yesandaero.domain.store.dto.MenuBulkUpdateResponse
import dsmhackathon18.yesandaero.domain.store.dto.MenuResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreDetailResponse
import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreAlreadyExistsException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.service.StoreService
import dsmhackathon18.yesandaero.global.exception.handler.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalTime

class StoreControllerTest {

    private val storeService = mockk<StoreService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(StoreController(storeService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            1L, null, listOf(SimpleGrantedAuthority("ROLE_OWNER")),
        )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private val requestJson = """
        {
          "name": "시흔식당",
          "category": "KOREAN",
          "address": "대전시 유성구",
          "latitude": 36.3624,
          "longitude": 127.3568,
          "phone": "042-000-0000",
          "avgPrice": 9000,
          "description": "백반 전문점",
          "openTime": "09:00",
          "closeTime": "21:00",
          "minOrderAmount": 8000,
          "menus": [
            { "name": "제육볶음", "description": "매콤한 제육", "price": 9000, "discountedPrice": 8000 }
          ]
        }
    """.trimIndent()

    @Test
    fun `가게 등록에 성공하면 201과 storeId를 반환한다`() {
        every { storeService.registerStore(1L, any()) } returns StoreRegisterResponse(storeId = 10L)

        mockMvc.perform(
            post("/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.storeId").value(10))
    }

    @Test
    fun `이미 가게가 등록되어 있으면 409와 STR_409를 반환한다`() {
        every { storeService.registerStore(1L, any()) } throws StoreAlreadyExistsException()

        mockMvc.perform(
            post("/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("STR_409"))
    }

    @Test
    fun `할인가가 정가보다 크면 400과 GLB_400을 반환한다`() {
        val invalidJson = """
            {
              "name": "시흔식당",
              "category": "KOREAN",
              "address": "대전시 유성구",
              "latitude": 36.3624,
              "longitude": 127.3568,
              "avgPrice": 9000,
              "openTime": "09:00",
              "closeTime": "21:00",
              "minOrderAmount": 8000,
              "menus": [
                { "name": "제육볶음", "price": 9000, "discountedPrice": 10000 }
              ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLB_400"))
    }

    private fun detailResponse(): StoreDetailResponse {
        val store = Store(
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
        return StoreDetailResponse.of(
            store = store,
            menus = listOf(MenuResponse(1L, "제육볶음", "매콤한 제육", 9000, 8000)),
            distance = null,
        )
    }

    @Test
    fun `가게 상세 조회에 성공하면 200과 가게 정보를 반환한다`() {
        every { storeService.getStoreDetail(10L, null, null) } returns detailResponse()

        mockMvc.perform(get("/stores/10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.storeId").value(10))
            .andExpect(jsonPath("$.menus[0].menuId").value(1))
            .andExpect(jsonPath("$.usableCouponCount").value(0))
    }

    @Test
    fun `존재하지 않는 가게를 조회하면 404와 STR_404를 반환한다`() {
        every { storeService.getStoreDetail(999L, null, null) } throws StoreNotFoundException()

        mockMvc.perform(get("/stores/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STR_404"))
    }

    @Test
    fun `내 가게 조회에 성공하면 200과 가게 정보를 반환한다`() {
        every { storeService.getMyStore(1L) } returns detailResponse()

        mockMvc.perform(get("/stores/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.storeId").value(10))
    }

    @Test
    fun `등록된 가게가 없으면 내 가게 조회 시 404와 STR_404를 반환한다`() {
        every { storeService.getMyStore(1L) } throws StoreNotFoundException()

        mockMvc.perform(get("/stores/me"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STR_404"))
    }

    @Test
    fun `가게 정보 수정에 성공하면 200과 수정된 정보를 반환한다`() {
        every { storeService.updateStore(1L, 10L, any()) } returns detailResponse()

        mockMvc.perform(
            patch("/stores/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avgPrice": 10000}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.storeId").value(10))
    }

    @Test
    fun `본인 가게가 아닌 가게를 수정하면 403과 STR_403을 반환한다`() {
        every { storeService.updateStore(1L, 10L, any()) } throws NotStoreOwnerException()

        mockMvc.perform(
            patch("/stores/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avgPrice": 10000}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("STR_403"))
    }

    @Test
    fun `존재하지 않는 가게를 수정하면 404와 STR_404를 반환한다`() {
        every { storeService.updateStore(1L, 999L, any()) } throws StoreNotFoundException()

        mockMvc.perform(
            patch("/stores/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avgPrice": 10000}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STR_404"))
    }

    @Test
    fun `메뉴 일괄 수정에 성공하면 200과 교체된 메뉴 목록을 반환한다`() {
        every { storeService.replaceMenus(1L, 10L, any()) } returns MenuBulkUpdateResponse(
            menus = listOf(
                MenuResponse(1L, "제육볶음", "매콤한 제육", 9000, 8000),
                MenuResponse(2L, "된장찌개", "집된장 사용", 8000, null),
            ),
        )

        mockMvc.perform(
            put("/stores/10/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "menus": [
                        { "name": "제육볶음", "description": "매콤한 제육", "price": 9000, "discountedPrice": 8000 },
                        { "name": "된장찌개", "description": "집된장 사용", "price": 8000, "discountedPrice": null }
                      ]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.menus.length()").value(2))
            .andExpect(jsonPath("$.menus[0].menuId").value(1))
            .andExpect(jsonPath("$.menus[1].discountedPrice").value(nullValue()))
    }

    @Test
    fun `할인가가 정가보다 크면 메뉴 일괄 수정 시 400과 GLB_400을 반환한다`() {
        mockMvc.perform(
            put("/stores/10/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "menus": [ { "name": "제육볶음", "price": 9000, "discountedPrice": 10000 } ] }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLB_400"))
    }

    @Test
    fun `본인 가게가 아닌 가게의 메뉴를 수정하면 403과 STR_403을 반환한다`() {
        every { storeService.replaceMenus(1L, 10L, any()) } throws NotStoreOwnerException()

        mockMvc.perform(
            put("/stores/10/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"menus": []}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("STR_403"))
    }
}
