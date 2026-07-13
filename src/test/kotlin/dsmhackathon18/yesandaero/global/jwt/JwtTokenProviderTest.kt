package dsmhackathon18.yesandaero.global.jwt

import dsmhackathon18.yesandaero.domain.user.entity.Role
import dsmhackathon18.yesandaero.global.jwt.exception.ExpiredOrInvalidTokenException
import dsmhackathon18.yesandaero.global.jwt.exception.MalformedTokenException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtTokenProviderTest {

    private val properties = JwtProperties(
        secret = "test-secret-key-for-jwt-token-provider-unit-test-0123456789",
        accessTokenExpiration = 3_600_000L,
        refreshTokenExpiration = 1_209_600_000L,
    )
    private val jwtTokenProvider = JwtTokenProvider(properties)

    @Test
    fun `accessToken을 발급하면 userId와 role을 담고 있다`() {
        val token = jwtTokenProvider.generateAccessToken(1L, Role.CUSTOMER)

        assertEquals(1L, jwtTokenProvider.getUserId(token))
        assertEquals(Role.CUSTOMER, jwtTokenProvider.getRole(token))
    }

    @Test
    fun `refreshToken을 발급하면 userId를 담고 있다`() {
        val token = jwtTokenProvider.generateRefreshToken(1L)

        assertEquals(1L, jwtTokenProvider.getUserId(token))
    }

    @Test
    fun `만료된 토큰을 파싱하면 ExpiredOrInvalidTokenException이 발생한다`() {
        val expiredProvider = JwtTokenProvider(properties.copy(accessTokenExpiration = -1_000L))
        val token = expiredProvider.generateAccessToken(1L, Role.CUSTOMER)

        assertFailsWith<ExpiredOrInvalidTokenException> {
            expiredProvider.parseClaims(token)
        }
    }

    @Test
    fun `형식이 올바르지 않은 토큰을 파싱하면 MalformedTokenException이 발생한다`() {
        assertFailsWith<MalformedTokenException> {
            jwtTokenProvider.parseClaims("not-a-jwt-token")
        }
    }
}
