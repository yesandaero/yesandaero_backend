package dsmhackathon18.yesandaero.domain.coupon.service

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponIssueResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponRegisterResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponStoreResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponUseResponse
import dsmhackathon18.yesandaero.domain.coupon.entity.Coupon
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import dsmhackathon18.yesandaero.domain.coupon.exception.CouponAlreadyRegisteredException
import dsmhackathon18.yesandaero.domain.coupon.exception.CouponNotFoundException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponStatusException
import dsmhackathon18.yesandaero.domain.coupon.exception.InvalidCouponTokenException
import dsmhackathon18.yesandaero.domain.coupon.exception.IssueNotAllowedException
import dsmhackathon18.yesandaero.domain.coupon.exception.NotCouponOwnerException
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
import java.time.LocalDateTime
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

    @Transactional
    fun useCoupon(userId: Long, couponId: Long): CouponUseResponse {
        val coupon = couponRepository.findById(couponId).orElseThrow { CouponNotFoundException() }
        if (coupon.userId != userId) {
            throw NotCouponOwnerException()
        }

        val now = LocalDateTime.now()
        val isExpired = coupon.status == CouponStatus.REGISTERED && coupon.expiresAt?.isBefore(now) == true
        if (coupon.status != CouponStatus.REGISTERED || isExpired) {
            throw InvalidCouponStatusException()
        }

        // 상태 조건부 UPDATE로 원자적/멱등하게 처리한다. 동시에 여러 요청이 들어와도
        // 정확히 하나만 성공한다(0건이면 이미 다른 요청이 먼저 처리한 것).
        val affected = couponRepository.useIfRegistered(couponId, now)
        if (affected == 0) {
            throw InvalidCouponStatusException()
        }

        val template = couponTemplateRepository.findById(coupon.templateId).orElseThrow { TemplateNotFoundException() }

        return CouponUseResponse(
            couponId = couponId,
            name = template.name,
            discountType = template.discountType,
            discountValue = template.discountValue,
            status = CouponStatus.USED,
            usedAt = now,
        )
    }
}
