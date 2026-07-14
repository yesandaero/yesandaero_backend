package dsmhackathon18.yesandaero.domain.coupon.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CouponIssueTokenRepositoryTest {

    private val connectionFactory = LettuceConnectionFactory(
        RedisStandaloneConfiguration("localhost", 6380).apply { password = RedisPassword.of("yesandaero") },
    ).apply { afterPropertiesSet() }

    private val redisTemplate = StringRedisTemplate(connectionFactory).apply { afterPropertiesSet() }
    private val repository = CouponIssueTokenRepository(redisTemplate)

    @AfterEach
    fun tearDown() {
        connectionFactory.destroy()
    }

    @Test
    fun `저장한 토큰을 소비하면 couponId를 반환하고 두 번째 소비는 null이다`() {
        val token = UUID.randomUUID().toString()
        repository.save(token, 42L, Duration.ofSeconds(60))

        val first = repository.consume(token)
        val second = repository.consume(token)

        assertEquals(42L, first)
        assertNull(second)
    }

    @Test
    fun `저장한 적 없는 토큰을 소비하면 null이다`() {
        val result = repository.consume(UUID.randomUUID().toString())

        assertNull(result)
    }

    @Test
    fun `동시에 같은 토큰을 소비하면 단 하나의 요청만 성공한다`() {
        val token = UUID.randomUUID().toString()
        repository.save(token, 1L, Duration.ofSeconds(60))

        val results = (1..20).toList().parallelStream()
            .map { repository.consume(token) }
            .toList()

        assertEquals(1, results.count { it == 1L })
        assertEquals(19, results.count { it == null })
    }
}
