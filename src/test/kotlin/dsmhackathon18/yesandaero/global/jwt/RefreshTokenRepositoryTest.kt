package dsmhackathon18.yesandaero.global.jwt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertEquals

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
    fun `refreshToken을 조회하면 저장된 값을 반환한다`() {
        every { valueOperations.get("refresh:1") } returns "token"

        val result = refreshTokenRepository.find(1L)

        assertEquals("token", result)
    }

    @Test
    fun `refreshToken을 삭제하면 해당 키를 삭제한다`() {
        every { redisTemplate.delete("refresh:1") } returns true

        refreshTokenRepository.delete(1L)

        verify { redisTemplate.delete("refresh:1") }
    }
}
