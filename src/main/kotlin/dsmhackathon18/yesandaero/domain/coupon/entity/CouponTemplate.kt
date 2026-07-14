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
@Table(name = "coupon_templates")
class CouponTemplate(
    @Column(name = "store_id", nullable = false)
    var storeId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    var discountType: DiscountType,

    @Column(name = "discount_value", nullable = false)
    var discountValue: Int,

    @Column(name = "min_order_amount", nullable = false)
    var minOrderAmount: Int,

    @Column(name = "valid_days", nullable = false)
    var validDays: Int,

    @Column(nullable = false)
    var active: Boolean = true,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun updateActive(active: Boolean?) {
        active?.let { this.active = it }
    }
}
