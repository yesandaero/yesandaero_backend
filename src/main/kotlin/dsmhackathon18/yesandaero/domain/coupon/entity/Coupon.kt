package dsmhackathon18.yesandaero.domain.coupon.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "coupons")
class Coupon(
    @Column(name = "template_id", nullable = false)
    var templateId: Long,

    @Column(name = "issuer_store_id", nullable = false)
    var issuerStoreId: Long,

    @Column(name = "target_store_id", nullable = false)
    var targetStoreId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CouponStatus = CouponStatus.ISSUED,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id")
    var userId: Long? = null
        protected set

    @Column(name = "used_store_id")
    var usedStoreId: Long? = null
        protected set

    @Column(name = "issued_at", nullable = false, updatable = false)
    var issuedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "registered_at")
    var registeredAt: LocalDateTime? = null
        protected set

    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null
        protected set

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null
        protected set

    fun register(userId: Long, validDays: Int) {
        val now = LocalDateTime.now()
        status = CouponStatus.REGISTERED
        this.userId = userId
        registeredAt = now
        expiresAt = now.plusDays(validDays.toLong())
    }

    fun expire() {
        status = CouponStatus.EXPIRED
    }
}
