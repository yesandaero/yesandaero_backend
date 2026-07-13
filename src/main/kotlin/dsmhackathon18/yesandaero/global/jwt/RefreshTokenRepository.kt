package dsmhackathon18.yesandaero.global.jwt

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RefreshTokenRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    fun save(userId: Long, refreshToken: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl)
    }

    fun find(userId: Long): String? =
        redisTemplate.opsForValue().get(key(userId))

    fun delete(userId: Long) {
        redisTemplate.delete(key(userId))
    }

    private fun key(userId: Long): String = "refresh:$userId"
}
