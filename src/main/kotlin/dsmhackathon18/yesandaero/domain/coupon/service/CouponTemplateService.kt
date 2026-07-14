package dsmhackathon18.yesandaero.domain.coupon.service

import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateCreateRequest
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateCreateResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateListResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateResponse
import dsmhackathon18.yesandaero.domain.coupon.dto.CouponTemplateUpdateRequest
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponTemplate
import dsmhackathon18.yesandaero.domain.coupon.exception.TemplateNotFoundException
import dsmhackathon18.yesandaero.domain.coupon.repository.CouponTemplateRepository
import dsmhackathon18.yesandaero.domain.store.exception.NotStoreOwnerException
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponTemplateService(
    private val couponTemplateRepository: CouponTemplateRepository,
    private val storeRepository: StoreRepository,
) {

    @Transactional
    fun createTemplate(ownerUserId: Long, request: CouponTemplateCreateRequest): CouponTemplateCreateResponse {
        val myStore = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()

        val template = couponTemplateRepository.save(
            CouponTemplate(
                storeId = requireNotNull(myStore.id),
                name = request.name,
                discountType = request.discountType,
                discountValue = request.discountValue,
                minOrderAmount = request.minOrderAmount,
                validDays = request.validDays,
                active = true,
            ),
        )

        return CouponTemplateCreateResponse(templateId = requireNotNull(template.id))
    }

    @Transactional
    fun updateTemplate(
        ownerUserId: Long,
        templateId: Long,
        request: CouponTemplateUpdateRequest,
    ): CouponTemplateResponse {
        val template = couponTemplateRepository.findById(templateId).orElseThrow { TemplateNotFoundException() }
        val store = storeRepository.findById(template.storeId).orElseThrow { StoreNotFoundException() }
        if (store.ownerUserId != ownerUserId) {
            throw NotStoreOwnerException()
        }

        template.updateActive(request.active)

        return CouponTemplateResponse.of(template)
    }

    @Transactional(readOnly = true)
    fun listMyTemplates(ownerUserId: Long, active: Boolean?): CouponTemplateListResponse {
        val myStore = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()
        val myStoreId = requireNotNull(myStore.id)

        val templates = if (active != null) {
            couponTemplateRepository.findAllByStoreIdAndActive(myStoreId, active)
        } else {
            couponTemplateRepository.findAllByStoreId(myStoreId)
        }

        return CouponTemplateListResponse(templates = templates.map(CouponTemplateResponse::of))
    }
}
