package dsmhackathon18.yesandaero.domain.coupon.service

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponStoreResponse
import dsmhackathon18.yesandaero.domain.coupon.entity.Coupon
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import dsmhackathon18.yesandaero.domain.coupon.exception.CouponAlreadyRegisteredException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponTokenException
import dsmhackathon18.yesandaero.domain.coupon.exception.IssueNotAllowedException
import dsmhackathon18.yesandaero.domain.coupon.exception.TemplateNotFoundException
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponIssueTokenRepository
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponRepository
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponTemplateRepository
import dsmhackathon18.yesandaero.domain.partnership.repository.PartnershipRepository
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponTemplateRepository: CouponTemplateRepository,
    private val couponIssueTokenRepository: CouponIssueTokenRepository,
    private val storeRepository: StoreRepository,
    private val partnershipRepository: PartnershipRepository,
) {

    companion object {
        private const val ISSUE_TOKEN_TTL_SECONDS = 600L
    }

    @Transactional
    fun issueCoupon(ownerUserId: Long, request: CouponIssueRequest): CouponIssueResponse {
        val myStore = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()
        val myStoreId = requireNotNull(myStore.id)

        val template = couponTemplateRepository.findById(request.templateId).orElseThrow { TemplateNotFoundException() }
        if (template.storeId != myStoreId || !template.active) {
            throw IssueNotAllowedException()
        }

        val targetStore = storeRepository.findById(request.targetStoreId).orElseThrow { StoreNotFoundException() }
        val targetStoreId = requireNotNull(targetStore.id)

        if (!partnershipRepository.existsAcceptedBetween(myStoreId, targetStoreId)) {
            throw IssueNotAllowedException()
        }

        val coupon = couponRepository.save(
            Coupon(
                templateId = requireNotNull(template.id),
                issuerStoreId = myStoreId,
                targetStoreId = targetStoreId,
                status = CouponStatus.ISSUED,
            ),
        )

        val token = UUID.randomUUID().toString()
        couponIssueTokenRepository.save(token, requireNotNull(coupon.id), Duration.ofSeconds(ISSUE_TOKEN_TTL_SECONDS))

        return CouponIssueResponse(
            couponId = requireNotNull(coupon.id),
            qrPayload = "couponapp://register?token=$token",
            expiresIn = ISSUE_TOKEN_TTL_SECONDS.toInt(),
        )
    }

    @Transactional
    fun registerCoupon(userId: Long, request: CouponRegisterRequest): CouponRegisterResponse {
        val couponId = couponIssueTokenRepository.consume(request.token) ?: throw InvalidCouponTokenException()
        val coupon = couponRepository.findById(couponId).orElseThrow { InvalidCouponTokenException() }
        if (coupon.status != CouponStatus.ISSUED) {
            throw CouponAlreadyRegisteredException()
        }

        val template = couponTemplateRepository.findById(coupon.templateId).orElseThrow { TemplateNotFoundException() }
        coupon.register(userId, template.validDays)

        val targetStore = storeRepository.findById(coupon.targetStoreId).orElseThrow { StoreNotFoundException() }

        return CouponRegisterResponse(
            couponId = requireNotNull(coupon.id),
            name = template.name,
            store = CouponStoreResponse(storeId = requireNotNull(targetStore.id), name = targetStore.name),
            status = coupon.status,
            expiresAt = requireNotNull(coupon.expiresAt),
        )
    }
}
