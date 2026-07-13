package dsmhackathon18.yesandaero.domain.store.controller

import dsmhackathon18.yesandaero.domain.store.dto.StoreRegisterResponse
import dsmhackathon18.yesandaero.domain.store.exception.StoreAlreadyExistsException
import dsmhackathon18.yesandaero.domain.store.service.StoreService
import dsmhackathon18.yesandaero.global.exception.handler.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

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
}
