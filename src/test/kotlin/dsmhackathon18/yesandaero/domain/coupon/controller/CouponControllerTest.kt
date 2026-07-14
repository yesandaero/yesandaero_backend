package dsmhackathon18.yesandaero.domain.coupon.controller

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponStoreResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponUseResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponWalletItemResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponWalletResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponWalletStoreResponse
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import dsmhackathon18.yesandaero.domain.coupon.entity.DiscountType
import dsmhackathon18.yesandaero.domain.coupon.exception.CouponAlreadyRegisteredException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponStatusException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponTokenException
import dsmhackathon18.yesandaero.domain.coupon.exception.IssueNotAllowedException
import dsmhackathon18.yesandaero.domain.coupon.exception.NotCouponOwnerException
import dsmhackathon18.yesandaero.domain.coupon.service.CouponService
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class CouponControllerTest {

    private val couponService = mockk<CouponService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(CouponController(couponService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticateAs(role: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            1L, null, listOf(SimpleGrantedAuthority("ROLE_$role")),
        )
    }

    @Test
    fun `쿠폰 발급에 성공하면 201과 qrPayload를 반환한다`() {
        authenticateAs("OWNER")
        every { couponService.issueCoupon(1L, any()) } returns CouponIssueResponse(
            couponId = 101L,
            qrPayload = "couponapp://register?token=abc",
            expiresIn = 600,
        )

        mockMvc.perform(
            post("/coupons/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"templateId": 3, "targetStoreId": 20}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.couponId").value(101))
            .andExpect(jsonPath("$.expiresIn").value(600))
    }

    @Test
    fun `발급 권한이 없으면 403과 CPN_403_01을 반환한다`() {
        authenticateAs("OWNER")
        every { couponService.issueCoupon(1L, any()) } throws IssueNotAllowedException()

        mockMvc.perform(
            post("/coupons/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"templateId": 3, "targetStoreId": 20}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("CPN_403_01"))
    }

    @Test
    fun `쿠폰 등록에 성공하면 200과 REGISTERED 상태를 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.registerCoupon(1L, any()) } returns CouponRegisterResponse(
            couponId = 101L,
            name = "아메리카노 1000원 할인",
            store = CouponStoreResponse(20L, "흔카페"),
            status = CouponStatus.REGISTERED,
            expiresAt = LocalDateTime.of(2026, 7, 27, 23, 59, 59),
        )

        mockMvc.perform(
            post("/coupons/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token": "8f3a"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REGISTERED"))
            .andExpect(jsonPath("$.store.storeId").value(20))
    }

    @Test
    fun `토큰이 유효하지 않으면 401과 CPN_401을 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.registerCoupon(1L, any()) } throws InvalidCouponTokenException()

        mockMvc.perform(
            post("/coupons/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token": "bad"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CPN_401"))
    }

    @Test
    fun `이미 등록된 쿠폰이면 409와 CPN_409_01을 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.registerCoupon(1L, any()) } throws CouponAlreadyRegisteredException()

        mockMvc.perform(
            post("/coupons/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token": "used-token"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CPN_409_01"))
    }

    @Test
    fun `token이 없으면 400과 GLB_400을 반환한다`() {
        authenticateAs("CUSTOMER")

        mockMvc.perform(
            post("/coupons/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLB_400"))
    }

    @Test
    fun `쿠폰 사용에 성공하면 200과 USED 상태를 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.useCoupon(1L, 101L) } returns CouponUseResponse(
            couponId = 101L,
            name = "아메리카노 1000원 할인",
            discountType = DiscountType.AMOUNT,
            discountValue = 1000,
            status = CouponStatus.USED,
            usedAt = LocalDateTime.of(2026, 7, 13, 12, 30),
        )

        mockMvc.perform(post("/coupons/101/use"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("USED"))
            .andExpect(jsonPath("$.discountValue").value(1000))
    }

    @Test
    fun `본인 쿠폰이 아니면 사용 시 403과 CPN_403_02를 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.useCoupon(1L, 101L) } throws NotCouponOwnerException()

        mockMvc.perform(post("/coupons/101/use"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("CPN_403_02"))
    }

    @Test
    fun `사용할 수 없는 상태의 쿠폰이면 409와 CPN_409_02를 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.useCoupon(1L, 101L) } throws InvalidCouponStatusException()

        mockMvc.perform(post("/coupons/101/use"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CPN_409_02"))
    }

    @Test
    fun `내 쿠폰함 조회에 성공하면 200과 목록을 반환한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.listMyCoupons(1L, null) } returns CouponWalletResponse(
            coupons = listOf(
                CouponWalletItemResponse(
                    couponId = 101L,
                    name = "아메리카노 1000원 할인",
                    store = CouponWalletStoreResponse(20L, "흔카페", StoreCategory.CAFE),
                    status = CouponStatus.REGISTERED,
                    expiresAt = LocalDateTime.of(2026, 7, 27, 23, 59, 59),
                ),
            ),
        )

        mockMvc.perform(get("/coupons/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.coupons[0].couponId").value(101))
            .andExpect(jsonPath("$.coupons[0].store.category").value("CAFE"))
    }

    @Test
    fun `내 쿠폰함 조회 시 status 쿼리로 필터링한다`() {
        authenticateAs("CUSTOMER")
        every { couponService.listMyCoupons(1L, CouponStatus.USED) } returns CouponWalletResponse(coupons = emptyList())

        mockMvc.perform(get("/coupons/me").param("status", "USED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.coupons.length()").value(0))
    }
}
