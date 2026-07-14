package dsmhackathon18.yesandaero.domain.coupon.controller

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateCreateRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateCreateResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateListResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateUpdateRequest
import dsmhackathon18.yesandaero.domain.coupon.service.CouponTemplateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coupon-templates")
class CouponTemplateController(
    private val couponTemplateService: CouponTemplateService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    fun createTemplate(
        @AuthenticationPrincipal ownerUserId: Long,
        @Valid @RequestBody request: CouponTemplateCreateRequest,
    ): CouponTemplateCreateResponse = couponTemplateService.createTemplate(ownerUserId, request)

    @PatchMapping("/{templateId}")
    @PreAuthorize("hasRole('OWNER')")
    fun updateTemplate(
        @AuthenticationPrincipal ownerUserId: Long,
        @PathVariable templateId: Long,
        @RequestBody request: CouponTemplateUpdateRequest,
    ): CouponTemplateResponse = couponTemplateService.updateTemplate(ownerUserId, templateId, request)

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    fun listTemplates(
        @AuthenticationPrincipal ownerUserId: Long,
        @RequestParam(required = false) ownerStoreId: Long?,
        @RequestParam(required = false) active: Boolean?,
    ): CouponTemplateListResponse = couponTemplateService.listTemplates(ownerUserId, ownerStoreId, active)
}
