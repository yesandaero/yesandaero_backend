package dsmhackathon18.yesandaero.domain.partnership.service

import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipCreateRequest
import dsmhackathon18.yesandaero.domain.partnership.entity.Partnership
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipAlreadyExistsException
import dsmhackathon18.yesandaero.domain.partnership.repository.PartnershipRepository
import dsmhackathon18.yesandaero.domain.store.entity.Store
import dsmhackathon18.yesandaero.domain.store.entity.StoreCategory
import dsmhackathon18.yesandaero.domain.store.exception.StoreNotFoundException
import dsmhackathon18.yesandaero.domain.store.repository.StoreRepository
import dsmhackathon18.yesandaero.global.exception.InvalidRequestException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PartnershipServiceTest {

    private val partnershipRepository = mockk<PartnershipRepository>()
    private val storeRepository = mockk<StoreRepository>()
    private val partnershipService = PartnershipService(partnershipRepository, storeRepository)

    private fun store(id: Long, ownerUserId: Long): Store =
        Store(
            ownerUserId = ownerUserId,
            name = "가게$id",
            category = StoreCategory.KOREAN,
            address = "대전시 유성구",
            phone = null,
            avgPrice = 8000,
            description = null,
            latitude = 36.3624,
            longitude = 127.3568,
            openTime = LocalTime.of(9, 0),
            closeTime = LocalTime.of(21, 0),
            minOrderAmount = 0,
        ).also { ReflectionTestUtils.setField(it, "id", id) }

    @Test
    fun `내 가게가 없으면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns null

        assertFailsWith<StoreNotFoundException> {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 2L))
        }
    }

    @Test
    fun `대상 가게가 없으면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<StoreNotFoundException> {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 999L))
        }
    }

    @Test
    fun `자기 자신의 가게에 요청하면 InvalidRequestException이 발생한다`() {
        val myStore = store(10L, 1L)
        every { storeRepository.findByOwnerUserId(1L) } returns myStore
        every { storeRepository.findById(10L) } returns Optional.of(myStore)

        assertFailsWith<InvalidRequestException> {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 10L))
        }
    }

    @Test
    fun `이미 같은 방향으로 진행 중인 제휴가 있으면 PartnershipAlreadyExistsException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(20L, 10L) } returns null
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(10L, 20L) } returns
            existingPartnership(10L, 20L, PartnershipStatus.PENDING)

        assertFailsWith<PartnershipAlreadyExistsException> {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 20L))
        }
    }

    @Test
    fun `반대 방향으로 이미 체결된 제휴가 있으면 PartnershipAlreadyExistsException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(20L, 10L) } returns
            existingPartnership(20L, 10L, PartnershipStatus.ACCEPTED)

        assertFailsWith<PartnershipAlreadyExistsException> {
            partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 20L))
        }

        verify(exactly = 0) { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(10L, 20L) }
    }

    @Test
    fun `REJECTED 상태의 기존 행이 있으면 재사용해서 PENDING으로 되돌린다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(20L, 10L) } returns null
        val existing = existingPartnership(10L, 20L, PartnershipStatus.REJECTED)
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(10L, 20L) } returns existing

        val response = partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 20L))

        assertEquals(PartnershipStatus.PENDING, response.status)
        assertEquals(PartnershipStatus.PENDING, existing.status)
        verify(exactly = 0) { partnershipRepository.save(any()) }
    }

    @Test
    fun `신규 제휴 요청에 성공하면 PENDING 상태로 저장한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(20L, 10L) } returns null
        every { partnershipRepository.findByRequesterStoreIdAndReceiverStoreId(10L, 20L) } returns null
        val savedSlot = slot<Partnership>()
        every { partnershipRepository.save(capture(savedSlot)) } answers {
            ReflectionTestUtils.setField(savedSlot.captured, "id", 100L)
            savedSlot.captured
        }

        val response = partnershipService.requestPartnership(1L, PartnershipCreateRequest(receiverStoreId = 20L))

        assertEquals(100L, response.partnershipId)
        assertEquals(PartnershipStatus.PENDING, response.status)
        assertEquals(10L, savedSlot.captured.requesterStoreId)
        assertEquals(20L, savedSlot.captured.receiverStoreId)
    }

    private fun existingPartnership(requesterStoreId: Long, receiverStoreId: Long, status: PartnershipStatus): Partnership =
        Partnership(requesterStoreId = requesterStoreId, receiverStoreId = receiverStoreId, status = status)
            .also { ReflectionTestUtils.setField(it, "id", 1L) }
}
