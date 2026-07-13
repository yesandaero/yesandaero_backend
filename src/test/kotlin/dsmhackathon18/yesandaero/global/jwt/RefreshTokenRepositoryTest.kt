package dsmhackathon18.yesandaero.global.jwt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshTokenRepositoryTest {

    private val redisTemplate = mockk<StringRedisTemplate>()
    private val valueOperations = mockk<ValueOperations<String, String>>()
    private val refreshTokenRepository = RefreshTokenRepository(redisTemplate)

    init {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    @Test
    fun `refreshToken을 저장하면 refresh 유저ID 키로 TTL과 함께 저장한다`() {
        every { valueOperations.set("refresh:1", "token", Duration.ofDays(14)) } returns Unit

        refreshTokenRepository.save(1L, "token", Duration.ofDays(14))

        verify { valueOperations.set("refresh:1", "token", Duration.ofDays(14)) }
    }

    @Test
    fun `refreshToken을 삭제하면 해당 키를 삭제한다`() {
        every { redisTemplate.delete("refresh:1") } returns true

        refreshTokenRepository.delete(1L)

        verify { redisTemplate.delete("refresh:1") }
    }

    @Test
    fun `저장된 값과 일치하면 원자적으로 새 값으로 교체하고 true를 반환한다`() {
        every {
            redisTemplate.execute<Long>(any(), listOf("refresh:1"), "old-token", "new-token", "1209600")
        } returns 1L

        val result = refreshTokenRepository.compareAndSwap(1L, "old-token", "new-token", Duration.ofDays(14))

        assertTrue(result)
    }

    @Test
    fun `저장된 값과 일치하지 않으면 교체하지 않고 false를 반환한다`() {
        every {
            redisTemplate.execute<Long>(any(), listOf("refresh:1"), "old-token", "new-token", "1209600")
        } returns 0L

        val result = refreshTokenRepository.compareAndSwap(1L, "old-token", "new-token", Duration.ofDays(14))

        assertFalse(result)
    }
}
