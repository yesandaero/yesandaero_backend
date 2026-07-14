package dsmhackathon18.yesandaero.domain.partnership.service

import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipCreateRequest
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipStatusResponse
import dsmhackathon18.yesandaero.domain.partnership.entity.Partnership
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipAlreadyExistsException
import dsmhackathon18.yesandaero.domain.partnership.repository.PartnershipRepository
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import dsmhackathon18.yesandaero.global.exception.InvalidRequestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PartnershipService(
    private val partnershipRepository: PartnershipRepository,
    private val storeRepository: StoreRepository,
) {

    companion object {
        private val ACTIVE_STATUSES = setOf(PartnershipStatus.PENDING, PartnershipStatus.ACCEPTED)
    }

    @Transactional
    fun requestPartnership(ownerUserId: Long, request: PartnershipCreateRequest): PartnershipStatusResponse {
        val myStore = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()
        val receiverStore = storeRepository.findById(request.receiverStoreId).orElseThrow { StoreNotFoundException() }

        val myStoreId = requireNotNull(myStore.id)
        val receiverStoreId = requireNotNull(receiverStore.id)

        if (myStoreId == receiverStoreId) {
            throw InvalidRequestException("자기 자신의 가게에는 제휴를 요청할 수 없습니다")
        }

        val reverse = partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(receiverStoreId, myStoreId)
        if (reverse != null && reverse.status in ACTIVE_STATUSES) {
            throw PartnershipAlreadyExistsException()
        }

        val existing = partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(myStoreId, receiverStoreId)
        if (existing != null) {
            if (existing.status in ACTIVE_STATUSES) {
                throw PartnershipAlreadyExistsException()
            }
            existing.reopen()
            return PartnershipStatusResponse.of(existing)
        }

        val saved = partnershipRepository.save(
            Partnership(
                requesterStoreId = myStoreId,
                receiverStoreId = receiverStoreId,
                status = PartnershipStatus.PENDING,
            ),
        )
        return PartnershipStatusResponse.of(saved)
    }
}
