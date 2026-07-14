package dsmhackathon18.yesandaero.domain.coupon.repository

import dsmhackathon18.yesandaero.domain.coupon.entity.CouponTemplate
import org.springframework.data.jpa.repository.JpaRepository

interface CouponTemplateRepository : JpaRepository<CouponTemplate, Long> {

    fun findAllByStoreId(storeId: Long): List<CouponTemplate>

    fun findAllByStoreIdAndActive(storeId: Long, active: Boolean): List<CouponTemplate>
}
