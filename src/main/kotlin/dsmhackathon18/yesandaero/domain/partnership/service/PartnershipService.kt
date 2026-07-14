package dsmhackathon18.yesandaero.domain.partnership.service

import dsmhackathon18.yesandaero.domain.partnership.dto.PartnerStoreResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipCreateRequest
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipDirection
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipItemResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipListResponse
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipStatusResponse
import dsmhackathon18.yesandaero.domain.partnership.entity.Partnership
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.partnership.exception.InvalidPartnershipStatusException
import dsmhackathon18.yesandaero.domain.partnership.exception.NotPartnershipPartyException
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipAlreadyExistsException
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipNotFoundException
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

    @Transactional(readOnly = true)
    fun listPartnerships(ownerUserId: Long, status: PartnershipStatus?): PartnershipListResponse {
        val myStore = storeRepository.findByOwnerUserId(ownerUserId) ?: throw StoreNotFoundException()
        val myStoreId = requireNotNull(myStore.id)

        val partnerships = partnershipRepository
            .findAllByRequesterStoreIdOrReceiverStoreId(myStoreId, myStoreId)
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }

        val partnerStoreIds = partnerships.map { partnerStoreId(it, myStoreId) }.distinct()
        val partnerStores = storeRepository.findAllById(partnerStoreIds).associateBy { requireNotNull(it.id) }

        val items = partnerships.mapNotNull { partnership ->
            val partnerStore = partnerStores[partnerStoreId(partnership, myStoreId)] ?: return@mapNotNull null
            PartnershipItemResponse(
                partnershipId = requireNotNull(partnership.id),
                partnerStore = PartnerStoreResponse(
                    storeId = requireNotNull(partnerStore.id),
                    name = partnerStore.name,
                    category = partnerStore.category,
                ),
                direction = if (partnership.requesterStoreId == myStoreId) {
                    PartnershipDirection.SENT
                } else {
                    PartnershipDirection.RECEIVED
                },
                status = partnership.status,
                createdAt = partnership.createdAt,
            )
        }

        return PartnershipListResponse(partnerships = items)
    }

    @Transactional
    fun acceptPartnership(ownerUserId: Long, partnershipId: Long): PartnershipStatusResponse {
        val partnership = partnershipRepository.findById(partnershipId).orElseThrow { PartnershipNotFoundException() }
        val receiverStore = storeRepository.findById(partnership.receiverStoreId).orElseThrow { StoreNotFoundException() }

        if (receiverStore.ownerUserId != ownerUserId) {
            throw NotPartnershipPartyException()
        }
        if (partnership.status != PartnershipStatus.PENDING) {
            throw InvalidPartnershipStatusException()
        }

        partnership.accept()
        return PartnershipStatusResponse.of(partnership)
    }

    @Transactional
    fun rejectPartnership(ownerUserId: Long, partnershipId: Long): PartnershipStatusResponse {
        val partnership = partnershipRepository.findById(partnershipId).orElseThrow { PartnershipNotFoundException() }
        val receiverStore = storeRepository.findById(partnership.receiverStoreId).orElseThrow { StoreNotFoundException() }

        if (receiverStore.ownerUserId != ownerUserId) {
            throw NotPartnershipPartyException()
        }
        if (partnership.status != PartnershipStatus.PENDING) {
            throw InvalidPartnershipStatusException()
        }

        partnership.reject()
        return PartnershipStatusResponse.of(partnership)
    }

    @Transactional
    fun terminatePartnership(ownerUserId: Long, partnershipId: Long) {
        val partnership = partnershipRepository.findById(partnershipId).orElseThrow { PartnershipNotFoundException() }
        val requesterStore = storeRepository.findById(partnership.requesterStoreId).orElseThrow { StoreNotFoundException() }
        val receiverStore = storeRepository.findById(partnership.receiverStoreId).orElseThrow { StoreNotFoundException() }

        if (requesterStore.ownerUserId != ownerUserId && receiverStore.ownerUserId != ownerUserId) {
            throw NotPartnershipPartyException()
        }
        if (partnership.status != PartnershipStatus.ACCEPTED) {
            throw InvalidPartnershipStatusException()
        }

        partnership.terminate()
    }

    private fun partnerStoreId(partnership: Partnership, myStoreId: Long): Long =
        if (partnership.requesterStoreId == myStoreId) partnership.receiverStoreId else partnership.requesterStoreId
}
