package dsmhackathon18.yesandaero.global.jwt

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RefreshTokenRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    fun save(userId: Long, refreshToken: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl)
    }

    fun delete(userId: Long) {
        redisTemplate.delete(key(userId))
    }

    /**
     * 저장된 값이 expectedToken과 일치할 때만 newToken으로 교체하는 compare-and-swap을
     * Lua 스크립트로 원자적으로 수행한다. 동일한 refreshToken으로 들어온 동시 요청 중
     * 하나만 성공하도록 보장한다.
     */
    fun compareAndSwap(userId: Long, expectedToken: String, newToken: String, ttl: Duration): Boolean {
        val result = redisTemplate.execute(
            COMPARE_AND_SWAP_SCRIPT,
            listOf(key(userId)),
            expectedToken,
            newToken,
            ttl.seconds.toString(),
        )
        return result == 1L
    }

    private fun key(userId: Long): String = "refresh:$userId"

    companion object {
        private val COMPARE_AND_SWAP_SCRIPT = DefaultRedisScript(
            """
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            else
                return 0
            end
            """.trimIndent(),
            Long::class.java,
        )
    }
}
