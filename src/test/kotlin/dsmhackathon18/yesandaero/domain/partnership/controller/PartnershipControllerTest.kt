package dsmhackathon18.yesandaero.domain.partnership.controller

import dsmhackathon18.yesandaero.domain.partnership.dto.PartnerStoreResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipCreateRequest
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipDirection
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipItemResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipListResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipStatusResponse
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipAlreadyExistsException
import dsmhackathon18.yesandaero.domain.partnership.service.PartnershipService
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
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

class PartnershipControllerTest {

    private val partnershipService = mockk<PartnershipService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(PartnershipController(partnershipService))
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

    @Test
    fun `제휴 요청에 성공하면 201과 PENDING 상태를 반환한다`() {
        every {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 20L))
        } returns PartnershipStatusResponse(partnershipId = 5L, status = PartnershipStatus.PENDING, acceptedAt = null)

        mockMvc.perform(
            post("/partnerships")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"receiverStoreId": 20}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.partnershipId").value(5))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.acceptedAt").doesNotExist())
    }

    @Test
    fun `대상 가게가 없으면 404와 STR_404를 반환한다`() {
        every {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 999L))
        } throws StoreNotFoundException()

        mockMvc.perform(
            post("/partnerships")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"receiverStoreId": 999}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STR_404"))
    }

    @Test
    fun `이미 진행 중인 제휴가 있으면 409와 PTN_409_01을 반환한다`() {
        every {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 20L))
        } throws PartnershipAlreadyExistsException()

        mockMvc.perform(
            post("/partnerships")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"receiverStoreId": 20}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("PTN_409_01"))
    }

    @Test
    fun `receiverStoreId가 없으면 400과 GLB_400을 반환한다`() {
        mockMvc.perform(
            post("/partnerships")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLB_400"))
    }

    @Test
    fun `제휴 목록 조회에 성공하면 200과 목록을 반환한다`() {
        every { partnershipService.listPartnerships(1L, null) } returns PartnershipListResponse(
            partnerships = listOf(
                PartnershipItemResponse(
                    partnershipId = 5L,
                    partnerStore = PartnerStoreResponse(12L, "흔카페", StoreCategory.CAFE),
                    direction = PartnershipDirection.RECEIVED,
                    status = PartnershipStatus.PENDING,
                    createdAt = LocalDateTime.of(2026, 7, 13, 10, 0),
                ),
            ),
        )

        mockMvc.perform(get("/partnerships"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.partnerships[0].partnershipId").value(5))
            .andExpect(jsonPath("$.partnerships[0].partnerStore.storeId").value(12))
            .andExpect(jsonPath("$.partnerships[0].direction").value("RECEIVED"))
    }

    @Test
    fun `제휴 목록 조회 시 status 쿼리로 필터링한다`() {
        every { partnershipService.listPartnerships(1L, PartnershipStatus.ACCEPTED) } returns
            PartnershipListResponse(partnerships = emptyList())

        mockMvc.perform(get("/partnerships").param("status", "ACCEPTED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.partnerships.length()").value(0))
    }
}
