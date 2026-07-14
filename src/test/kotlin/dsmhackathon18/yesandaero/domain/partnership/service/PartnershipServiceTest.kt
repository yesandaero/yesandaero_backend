package dsmhackathon18.yesandaero.domain.partnership.service

import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipCreateRequest
import dsmhackathon18.yesandaero.domain.partnership.dto.PartnershipDirection
import dsmhackathon18.yesandaero.domain.partnership.entity.Partnership
import dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus
import dsmhackathon18.yesandaero.domain.partnership.exception.InvalidPartnershipStatusException
import dsmhackathon18.yesandaero.domain.partnership.exception.NotPartnershipPartyException
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipAlreadyExistsException
import dsmhackathon18.yesandaero.domain.partnership.exception.PartnershipNotFoundException
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
import kotlin.test.assertNotNull

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

    @Test
    fun `제휴 목록 조회 시 내 가게가 없으면 StoreNotFoundException이 발생한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns null

        assertFailsWith<StoreNotFoundException> {
            partnershipService.listPartnerships(1L, null)
        }
    }

    @Test
    fun `제휴 목록 조회 시 보낸 요청과 받은 요청의 방향을 올바르게 구분한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        val sent = existingPartnership(10L, 20L, PartnershipStatus.PENDING, id = 1L)
        val received = existingPartnership(30L, 10L, PartnershipStatus.ACCEPTED, id = 2L)
        every {
            partnershipRepository.findAllByRequesterStoreIdOrReceiverStoreId(10L, 10L)
        } returns listOf(sent, received)
        every {
            storeRepository.findAllById(any<List<Long>>())
        } returns listOf(store(20L, 2L), store(30L, 3L))

        val response = partnershipService.listPartnerships(1L, null)

        assertEquals(2, response.partnerships.size)
        val sentItem = response.partnerships.first { it.partnershipId == 1L }
        val receivedItem = response.partnerships.first { it.partnershipId == 2L }
        assertEquals(PartnershipDirection.SENT, sentItem.direction)
        assertEquals(20L, sentItem.partnerStore.storeId)
        assertEquals(PartnershipDirection.RECEIVED, receivedItem.direction)
        assertEquals(30L, receivedItem.partnerStore.storeId)
    }

    @Test
    fun `제휴 목록 조회 시 status로 필터링한다`() {
        every { storeRepository.findByOwnerUserId(1L) } returns store(10L, 1L)
        val pending = existingPartnership(10L, 20L, PartnershipStatus.PENDING, id = 1L)
        val accepted = existingPartnership(10L, 30L, PartnershipStatus.ACCEPTED, id = 2L)
        every {
            partnershipRepository.findAllByRequesterStoreIdOrReceiverStoreId(10L, 10L)
        } returns listOf(pending, accepted)
        every { storeRepository.findAllById(listOf(30L)) } returns listOf(store(30L, 3L))

        val response = partnershipService.listPartnerships(1L, PartnershipStatus.ACCEPTED)

        assertEquals(1, response.partnerships.size)
        assertEquals(2L, response.partnerships[0].partnershipId)
    }

    @Test
    fun `제휴가 존재하지 않으면 수락 시 PartnershipNotFoundException이 발생한다`() {
        every { partnershipRepository.findById(5L) } returns Optional.empty()

        assertFailsWith<PartnershipNotFoundException> {
            partnershipService.acceptPartnership(1L, 5L)
        }
    }

    @Test
    fun `수신 가게 소유주가 아니면 수락 시 NotPartnershipPartyException이 발생한다`() {
        val partnership = existingPartnership(10L, 20L, PartnershipStatus.PENDING)
        every { partnershipRepository.findById(1L) } returns Optional.of(partnership)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 999L))

        assertFailsWith<NotPartnershipPartyException> {
            partnershipService.acceptPartnership(1L, 1L)
        }
    }

    @Test
    fun `PENDING이 아니면 수락 시 InvalidPartnershipStatusException이 발생한다`() {
        val partnership = existingPartnership(10L, 20L, PartnershipStatus.ACCEPTED)
        every { partnershipRepository.findById(1L) } returns Optional.of(partnership)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))

        assertFailsWith<InvalidPartnershipStatusException> {
            partnershipService.acceptPartnership(2L, 1L)
        }
    }

    @Test
    fun `제휴 수락에 성공하면 ACCEPTED로 전이하고 acceptedAt을 기록한다`() {
        val partnership = existingPartnership(10L, 20L, PartnershipStatus.PENDING)
        every { partnershipRepository.findById(1L) } returns Optional.of(partnership)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))

        val response = partnershipService.acceptPartnership(2L, 1L)

        assertEquals(PartnershipStatus.ACCEPTED, response.status)
        assertEquals(PartnershipStatus.ACCEPTED, partnership.status)
        assertEquals(partnership.acceptedAt, response.acceptedAt)
        assertNotNull(response.acceptedAt)
    }

    @Test
    fun `제휴가 존재하지 않으면 거절 시 PartnershipNotFoundException이 발생한다`() {
        every { partnershipRepository.findById(5L) } returns Optional.empty()

        assertFailsWith<PartnershipNotFoundException> {
            partnershipService.rejectPartnership(1L, 5L)
        }
    }

    @Test
    fun `수신 가게 소유주가 아니면 거절 시 NotPartnershipPartyException이 발생한다`() {
        val partnership = existingPartnership(10L, 20L, PartnershipStatus.PENDING)
        every { partnershipRepository.findById(1L) } returns Optional.of(partnership)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 999L))

        assertFailsWith<NotPartnershipPartyException> {
            partnershipService.rejectPartnership(1L, 1L)
        }
    }

    @Test
    fun `PENDING이 아니면 거절 시 InvalidPartnershipStatusException이 발생한다`() {
        val partnership = existingPartnership(10L, 20L, PartnershipStatus.TERMINATED)
        every { partnershipRepository.findById(1L) } returns Optional.of(partnership)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))

        assertFailsWith<InvalidPartnershipStatusException> {
            partnershipService.rejectPartnership(2L, 1L)
        }
    }

    @Test
    fun `제휴 거절에 성공하면 REJECTED로 전이한다`() {
        val partnership = existingPartnership(10L, 20L, PartnershipStatus.PENDING)
        every { partnershipRepository.findById(1L) } returns Optional.of(partnership)
        every { storeRepository.findById(20L) } returns Optional.of(store(20L, 2L))

        val response = partnershipService.rejectPartnership(2L, 1L)

        assertEquals(PartnershipStatus.REJECTED, response.status)
        assertEquals(null, response.acceptedAt)
    }

    private fun existingPartnership(
        requesterStoreId: Long,
        receiverStoreId: Long,
        status: PartnershipStatus,
        id: Long = 1L,
    ): Partnership =
        Partnership(requesterStoreId = requesterStoreId, receiverStoreId = receiverStoreId, status = status)
            .also { ReflectionTestUtils.setField(it, "id", id) }
}
