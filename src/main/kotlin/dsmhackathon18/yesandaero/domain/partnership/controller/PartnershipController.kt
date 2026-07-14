package dsmhackathon18.yesandaero.domain.partnership.controller

import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipCreateRequest
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipListResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipStatusResponse
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.partnership.service.PartnershipService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/partnerships")
class PartnershipController(
    private val partnershipService: PartnershipService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    fun requestPartnership(
        @AuthenticationPrincipal ownerUserId: Long,
        @Valid @RequestBody request: PartnershipCreateRequest,
    ): PartnershipStatusResponse = partnershipService.requestPartnership(ownerUserId, request)

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    fun listPartnerships(
        @AuthenticationPrincipal ownerUserId: Long,
        @RequestParam(required = false) status: PartnershipStatus?,
    ): PartnershipListResponse = partnershipService.listPartnerships(ownerUserId, status)
}
