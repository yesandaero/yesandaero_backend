package dsmhackathon18.yesandaero.domain.coupon.repository

import dsmhackathon18.yesandaero.domain.coupon.entity.Coupon
import dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface CouponRepository : JpaRepository<Coupon, Long> {

    fun findAllByUserId(userId: Long): List<Coupon>

    fun findAllByUserIdAndStatus(userId: Long, status: CouponStatus): List<Coupon>

    /**
     * 상태 조건부 UPDATE로 쿠폰 사용을 원자적으로 처리한다. REGISTERED 상태일 때만
     * USED로 전이하며, 영향받은 row 수(0 또는 1)로 성공 여부를 판별한다(멱등 처리).
     */
    @Modifying
    @Query(
        """
        UPDATE Coupon c
        SET c.status = dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus.USED,
            c.usedStoreId = c.targetStoreId,
            c.usedAt = :usedAt
        WHERE c.id = :couponId AND c.status = dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus.REGISTERED
        """,
    )
    fun useIfRegistered(@Param("couponId") couponId: Long, @Param("usedAt") usedAt: LocalDateTime): Int

    @Modifying
    @Query(
        """
        UPDATE Coupon c
        SET c.status = dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus.EXPIRED
        WHERE c.userId = :userId
        AND c.status = dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus.REGISTERED
        AND c.expiresAt < :now
        """,
    )
    fun expireOverdueCoupons(@Param("userId") userId: Long, @Param("now") now: LocalDateTime): Int

    @Query(
        """
        SELECT COUNT(c) FROM Coupon c
        WHERE c.userId = :userId AND c.targetStoreId = :storeId
        AND c.status = dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus.REGISTERED
        AND c.expiresAt > :now
        """,
    )
    fun countUsable(@Param("userId") userId: Long, @Param("storeId") storeId: Long, @Param("now") now: LocalDateTime): Long

    @Query(
        """
        SELECT DISTINCT c.targetStoreId FROM Coupon c
        WHERE c.userId = :userId AND c.targetStoreId IN :storeIds
        AND c.status = dsmhackathon18.yesandaero.domain.coupon.entity.CouponStatus.REGISTERED
        AND c.expiresAt > :now
        """,
    )
    fun findTargetStoreIdsWithUsableCoupon(
        @Param("userId") userId: Long,
        @Param("storeIds") storeIds: List<Long>,
        @Param("now") now: LocalDateTime,
    ): List<Long>
}
