package dsmhackathon18.yesandaero.domain.partnership.repository

import dsmhackathon18.yesandaero.domain.partnership.entity.Partnership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PartnershipRepository : JpaRepository<Partnership, Long> {

    fun findByRequesterStoreIdAndReceiverStoreId(requesterStoreId: Long, receiverStoreId: Long): Partnership?

    fun findAllByRequesterStoreIdOrReceiverStoreId(requesterStoreId: Long, receiverStoreId: Long): List<Partnership>

    @Query(
        """
        SELECT p FROM Partnership p
        WHERE (p.requesterStoreId = :myStoreId AND p.receiverStoreId IN :targetStoreIds)
        OR (p.receiverStoreId = :myStoreId AND p.requesterStoreId IN :targetStoreIds)
        """,
    )
    fun findAllBetween(
        @Param("myStoreId") myStoreId: Long,
        @Param("targetStoreIds") targetStoreIds: List<Long>,
    ): List<Partnership>

    @Query(
        """
        SELECT COUNT(p) > 0 FROM Partnership p
        WHERE p.status = dsmhackathon18.yesandaero.domain.partnership.entity.PartnershipStatus.ACCEPTED
        AND ((p.requesterStoreId = :storeA AND p.receiverStoreId = :storeB)
          OR (p.requesterStoreId = :storeB AND p.receiverStoreId = :storeA))
        """,
    )
    fun existsAcceptedBetween(@Param("storeA") storeA: Long, @Param("storeB") storeB: Long): Boolean
}
