package dsmhackathon18.yesandaero.domain.coupon.controller

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterResponse
import dsmhackathon18.yesandaero.domain.coupon.service.CouponService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coupons")
class CouponController(
    private val couponService: CouponService,
) {

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    fun issueCoupon(
        @AuthenticationPrincipal ownerUserId: Long,
        @Valid @RequestBody request: CouponIssueRequest,
    ): CouponIssueResponse = couponService.issueCoupon(ownerUserId, request)

    @PostMapping("/register")
    @PreAuthorize("hasRole('CUSTOMER')")
    fun registerCoupon(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CouponRegisterRequest,
    ): CouponRegisterResponse = couponService.registerCoupon(userId, request)
}
