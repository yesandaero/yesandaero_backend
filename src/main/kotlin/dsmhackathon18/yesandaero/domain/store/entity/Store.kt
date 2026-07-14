package dsmhackathon18.yesandaero.domain.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(
    name = "stores",
    indexes = [Index(name = "idx_stores_lat_lng", columnList = "latitude, longitude")],
)
class Store(
    @Column(name = "owner_user_id", nullable = false, unique = true)
    var ownerUserId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: StoreCategory,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(length = 20)
    var phone: String?,

    @Column(name = "avg_price", nullable = false)
    var avgPrice: Int,

    @Column(length = 500)
    var description: String?,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(name = "open_time", nullable = false)
    var openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    var closeTime: LocalTime,

    @Column(name = "min_order_amount", nullable = false)
    var minOrderAmount: Int,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun update(
        name: String?,
        category: StoreCategory?,
        address: String?,
        phone: String?,
        avgPrice: Int?,
        description: String?,
        latitude: Double?,
        longitude: Double?,
        openTime: LocalTime?,
        closeTime: LocalTime?,
        minOrderAmount: Int?,
    ) {
        name?.let { this.name = it }
        category?.let { this.category = it }
        address?.let { this.address = it }
        phone?.let { this.phone = it }
        avgPrice?.let { this.avgPrice = it }
        description?.let { this.description = it }
        latitude?.let { this.latitude = it }
        longitude?.let { this.longitude = it }
        openTime?.let { this.openTime = it }
        closeTime?.let { this.closeTime = it }
        minOrderAmount?.let { this.minOrderAmount = it }
    }
}
