package dsmhackathon18.yesandaero.domain.statistics.controller

import dsmhackathon18.yesandaero.domain.statistics.dto.StoreStatisticsResponse
import dsmhackathon18.yesandaero.domain.statistics.service.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class StatisticsController(
    private val statisticsService: StatisticsService,
) {

    @GetMapping("/stores/{storeId}/statistics")
    @PreAuthorize("hasRole('OWNER')")
    fun getStoreStatistics(
        @AuthenticationPrincipal ownerUserId: Long,
        @PathVariable storeId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): StoreStatisticsResponse = statisticsService.getStoreStatistics(ownerUserId, storeId, from, to)
}
