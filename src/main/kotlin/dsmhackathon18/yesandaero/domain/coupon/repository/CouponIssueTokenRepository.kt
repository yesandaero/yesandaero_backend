package dsmhackathon18.yesandaero.domain.coupon.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class CouponIssueTokenRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    fun save(token: String, couponId: Long, ttl: Duration) {
        redisTemplate.opsForValue().set(key(token), couponId.toString(), ttl)
    }

    /**
     * 토큰을 원자적으로 조회 후 삭제한다(GETDEL). 동시에 같은 토큰으로 들어온 요청 중
     * 하나만 couponId를 얻고, 나머지는 null을 받는다.
     */
    fun consume(token: String): Long? =
        redisTemplate.opsForValue().getAndDelete(key(token))?.toLongOrNull()

    private fun key(token: String): String = "coupon:issue:$token"
}
