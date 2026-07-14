package dsmhackathon18.yesandaero.domain.coupon.controller

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateCreateResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateListResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateResponse
import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType
import dsmhackathon18.yesandaero.domain.coupon.exception.TemplateNotFoundException
import dsmhackathon18.yesandaero.domain.coupon.service.CouponTemplateService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class CouponTemplateControllerTest {

    private val couponTemplateService = mockk<CouponTemplateService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(CouponTemplateController(couponTemplateService))
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

    private val validCreateJson = """
        {
          "name": "아메리카노 1000원 할인",
          "discountType": "AMOUNT",
          "discountValue": 1000,
          "minOrderAmount": 5000,
          "validDays": 14
        }
    """.trimIndent()

    @Test
    fun `템플릿 생성에 성공하면 201과 templateId를 반환한다`() {
        every { couponTemplateService.createTemplate(1L, any()) } returns CouponTemplateCreateResponse(templateId = 3L)

        mockMvc.perform(
            post("/coupon-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateId").value(3))
    }

    @Test
    fun `RATE 할인이 100을 초과하면 400과 GLB_400을 반환한다`() {
        val invalidJson = """
            {
              "name": "50% 할인",
              "discountType": "RATE",
              "discountValue": 150,
              "minOrderAmount": 5000,
              "validDays": 14
            }
        """.trimIndent()

        mockMvc.perform(
            post("/coupon-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLB_400"))
    }

    @Test
    fun `템플릿 수정에 성공하면 200과 수정된 템플릿을 반환한다`() {
        every { couponTemplateService.updateTemplate(1L, 3L, any()) } returns CouponTemplateResponse(
            templateId = 3L,
            name = "아메리카노 1000원 할인",
            discountType = DiscountType.AMOUNT,
            discountValue = 1000,
            minOrderAmount = 5000,
            validDays = 14,
            active = false,
        )

        mockMvc.perform(
            patch("/coupon-templates/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"active": false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))
    }

    @Test
    fun `존재하지 않는 템플릿을 수정하면 404와 CPN_404_01을 반환한다`() {
        every { couponTemplateService.updateTemplate(1L, 999L, any()) } throws TemplateNotFoundException()

        mockMvc.perform(
            patch("/coupon-templates/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"active": false}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CPN_404_01"))
    }

    @Test
    fun `내 템플릿 목록 조회에 성공하면 200을 반환한다`() {
        every { couponTemplateService.listMyTemplates(1L, null) } returns CouponTemplateListResponse(
            templates = listOf(
                CouponTemplateResponse(3L, "아메리카노 1000원 할인", DiscountType.AMOUNT, 1000, 5000, 14, true),
            ),
        )

        mockMvc.perform(get("/coupon-templates"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.templates[0].templateId").value(3))
    }
}
