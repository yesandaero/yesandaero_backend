package dsmhackathon18.yesandaero.domain.auth.service

import dsmhackathon18.yesandaero.domain.auth.dto.LoginRequest
import dsmhackathon18.yesandaero.domain.auth.dto.RefreshRequest
import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.user.entity.Role
import dsmhackathon18.yesandaero.domain.user.entity.User
import dsmhackathon18.yesandaero.domain.user.exception.DuplicateEmailException
import dsmhackathon18.yesandaero.domain.user.exception.LoginFailedException
import dsmhackathon18.yesandaero.domain.user.repository.UserRepository
import dsmhackathon18.yesandaero.global.jwt.JwtTokenProvider
import dsmhackathon18.yesandaero.global.jwt.RefreshTokenRepository
import dsmhackathon18.yesandaero.global.jwt.exception.ExpiredOrInvalidTokenException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.time.Duration
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val authService = AuthService(userRepository, passwordEncoder, jwtTokenProvider, refreshTokenRepository)

    @Test
    fun `이미 가입된 이메일로 회원가입하면 DuplicateEmailException이 발생한다`() {
        every { userRepository.existsByEmail("user@example.com") } returns true

        assertFailsWith<DuplicateEmailException> {
            authService.signup(SignupRequest("kimsiheun", "user@example.com", "P@ssw0rd!", Role.CUSTOMER))
        }

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `회원가입에 성공하면 비밀번호를 암호화해 저장하고 저장된 유저의 id를 반환한다`() {
        every { userRepository.existsByEmail("user@example.com") } returns false
        every { passwordEncoder.encode("P@ssw0rd!") } returns "encoded-password"
        val savedUser = slot<User>()
        every { userRepository.save(capture(savedUser)) } answers {
            ReflectionTestUtils.setField(savedUser.captured, "id", 1L)
            savedUser.captured
        }

        val response = authService.signup(SignupRequest("kimsiheun", "user@example.com", "P@ssw0rd!", Role.CUSTOMER))

        assertEquals(1L, response.userId)
        assertEquals("encoded-password", savedUser.captured.passwordHash)
        assertEquals("user@example.com", savedUser.captured.email)
        assertEquals(Role.CUSTOMER, savedUser.captured.role)
    }

    @Test
    fun `존재하지 않는 이메일로 로그인하면 LoginFailedException이 발생한다`() {
        every { userRepository.findByEmail("user@example.com") } returns null

        assertFailsWith<LoginFailedException> {
            authService.login(LoginRequest("user@example.com", "P@ssw0rd!"))
        }
    }

    @Test
    fun `비밀번호가 일치하지 않으면 LoginFailedException이 발생한다`() {
        val user = existingUser()
        every { userRepository.findByEmail("user@example.com") } returns user
        every { passwordEncoder.matches("wrong-password", "encoded-password") } returns false

        assertFailsWith<LoginFailedException> {
            authService.login(LoginRequest("user@example.com", "wrong-password"))
        }
    }

    @Test
    fun `로그인에 성공하면 accessToken과 refreshToken을 발급하고 refreshToken을 Redis에 저장한다`() {
        val user = existingUser()
        every { userRepository.findByEmail("user@example.com") } returns user
        every { passwordEncoder.matches("P@ssw0rd!", "encoded-password") } returns true
        every { jwtTokenProvider.generateAccessToken(1L, Role.CUSTOMER) } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(1L) } returns "refresh-token"
        every { jwtTokenProvider.refreshTokenTtl } returns Duration.ofDays(14)
        every { refreshTokenRepository.save(1L, "refresh-token", Duration.ofDays(14)) } returns Unit

        val response = authService.login(LoginRequest("user@example.com", "P@ssw0rd!"))

        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals(Role.CUSTOMER, response.role)
        verify { refreshTokenRepository.save(1L, "refresh-token", Duration.ofDays(14)) }
    }

    @Test
    fun `로그아웃하면 Redis에 저장된 refreshToken을 삭제한다`() {
        every { refreshTokenRepository.delete(1L) } returns Unit

        authService.logout(1L)

        verify { refreshTokenRepository.delete(1L) }
    }

    @Test
    fun `refreshToken의 유저를 찾을 수 없으면 ExpiredOrInvalidTokenException이 발생한다`() {
        every { jwtTokenProvider.getUserId("refresh-token") } returns 1L
        every { userRepository.findById(1L) } returns Optional.empty()

        assertFailsWith<ExpiredOrInvalidTokenException> {
            authService.refresh(RefreshRequest("refresh-token"))
        }

        verify(exactly = 0) { refreshTokenRepository.compareAndSwap(any(), any(), any(), any()) }
    }

    @Test
    fun `저장된 refreshToken과 일치하지 않아 원자적 교체에 실패하면 ExpiredOrInvalidTokenException이 발생한다`() {
        val user = existingUser()
        every { jwtTokenProvider.getUserId("refresh-token") } returns 1L
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { jwtTokenProvider.generateAccessToken(1L, Role.CUSTOMER) } returns "new-access-token"
        every { jwtTokenProvider.generateRefreshToken(1L) } returns "new-refresh-token"
        every { jwtTokenProvider.refreshTokenTtl } returns Duration.ofDays(14)
        every {
            refreshTokenRepository.compareAndSwap(1L, "refresh-token", "new-refresh-token", Duration.ofDays(14))
        } returns false

        assertFailsWith<ExpiredOrInvalidTokenException> {
            authService.refresh(RefreshRequest("refresh-token"))
        }
    }

    @Test
    fun `토큰 재발급에 성공하면 accessToken과 refreshToken을 새로 발급하고 원자적으로 회전한다`() {
        val user = existingUser()
        every { jwtTokenProvider.getUserId("refresh-token") } returns 1L
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { jwtTokenProvider.generateAccessToken(1L, Role.CUSTOMER) } returns "new-access-token"
        every { jwtTokenProvider.generateRefreshToken(1L) } returns "new-refresh-token"
        every { jwtTokenProvider.refreshTokenTtl } returns Duration.ofDays(14)
        every {
            refreshTokenRepository.compareAndSwap(1L, "refresh-token", "new-refresh-token", Duration.ofDays(14))
        } returns true

        val response = authService.refresh(RefreshRequest("refresh-token"))

        assertEquals("new-access-token", response.accessToken)
        assertEquals("new-refresh-token", response.refreshToken)
        verify { refreshTokenRepository.compareAndSwap(1L, "refresh-token", "new-refresh-token", Duration.ofDays(14)) }
    }

    private fun existingUser(): User =
        User(
            username = "kimsiheun",
            passwordHash = "encoded-password",
            email = "user@example.com",
            role = Role.CUSTOMER,
        ).also { ReflectionTestUtils.setField(it, "id", 1L) }
}
