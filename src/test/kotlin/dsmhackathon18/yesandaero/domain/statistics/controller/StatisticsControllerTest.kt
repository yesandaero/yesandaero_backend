package dsmhackathon18.yesandaero.domain.statistics.controller

import dsmhackathon18.yesandaero.domain.statistics.dto.IssuedStatsResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.RedeemedByIssuerStoreResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.RedeemedStatsResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.StatisticsPeriodResponse
import dsmhackathon18.yesandaero.domain.statistics.dto.StoreStatisticsResponse
import dsmhackathon18.yesandaero.domain.statistics.service.StatisticsService
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.global.exception.handler.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

class StatisticsControllerTest {

    private val statisticsService = mockk<StatisticsService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(StatisticsController(statisticsService))
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
    fun `가게 통계 조회에 성공하면 200과 집계 결과를 반환한다`() {
        authenticateAs("OWNER")
        every {
            statisticsService.getStoreStatistics(1L, 10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 13))
        } returns StoreStatisticsResponse(
            period = StatisticsPeriodResponse(from = LocalDate.of(2026, 7, 1), to = LocalDate.of(2026, 7, 13)),
            issued = IssuedStatsResponse(total = 120, registered = 80, used = 45),
            redeemedAtMyStore = RedeemedStatsResponse(
                total = 30,
                byIssuerStore = listOf(
                    RedeemedByIssuerStoreResponse(storeId = 12L, name = "흔카페", count = 18),
                    RedeemedByIssuerStoreResponse(storeId = 15L, name = "유성분식", count = 12),
                ),
            ),
        )

        mockMvc.perform(
            get("/stores/10/statistics")
                .param("from", "2026-07-01")
                .param("to", "2026-07-13"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.issued.total").value(120))
            .andExpect(jsonPath("$.redeemedAtMyStore.total").value(30))
            .andExpect(jsonPath("$.redeemedAtMyStore.byIssuerStore[0].storeId").value(12))
            .andExpect(jsonPath("$.redeemedAtMyStore.byIssuerStore[0].name").value("흔카페"))
    }

    @Test
    fun `본인 가게가 아니면 403과 STR_403을 반환한다`() {
        authenticateAs("OWNER")
        every { statisticsService.getStoreStatistics(1L, 10L, any(), any()) } throws NotStoreOwnerException()

        mockMvc.perform(
            get("/stores/10/statistics")
                .param("from", "2026-07-01")
                .param("to", "2026-07-13"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("STR_403"))
    }

    @Test
    fun `존재하지 않는 가게면 404와 STR_404를 반환한다`() {
        authenticateAs("OWNER")
        every { statisticsService.getStoreStatistics(1L, 10L, any(), any()) } throws StoreNotFoundException()

        mockMvc.perform(
            get("/stores/10/statistics")
                .param("from", "2026-07-01")
                .param("to", "2026-07-13"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STR_404"))
    }

    @Test
    fun `from-to 쿼리 파라미터가 없으면 400을 반환한다`() {
        authenticateAs("OWNER")

        mockMvc.perform(get("/stores/10/statistics"))
            .andExpect(status().isBadRequest)
    }
}
