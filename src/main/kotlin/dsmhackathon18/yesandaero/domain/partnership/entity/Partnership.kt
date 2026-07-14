package dsmhackathon18.yesandaero.domain.partnership.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "partnerships",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["requester_store_id", "receiver_store_id"]),
    ],
)
class Partnership(
    @Column(name = "requester_store_id", nullable = false)
    var requesterStoreId: Long,

    @Column(name = "receiver_store_id", nullable = false)
    var receiverStoreId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PartnershipStatus,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "accepted_at")
    var acceptedAt: LocalDateTime? = null
        protected set

    fun accept() {
        status = PartnershipStatus.ACCEPTED
        acceptedAt = LocalDateTime.now()
    }

    fun reject() {
        status = PartnershipStatus.REJECTED
    }

    fun terminate() {
        status = PartnershipStatus.TERMINATED
    }

    // REJECTED/TERMINATED 상태였던 기존 행을 재요청 시 재사용한다 (UNIQUE 제약 충돌 방지).
    fun reopen() {
        status = PartnershipStatus.PENDING
        acceptedAt = null
    }
}
