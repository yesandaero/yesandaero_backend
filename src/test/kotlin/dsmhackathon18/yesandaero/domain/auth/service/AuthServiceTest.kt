package dsmhackathon18.yesandaero.domain.auth.service

import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.user.entity.Role
import dsmhackathon18.yesandaero.domain.user.entity.User
import dsmhackathon18.yesandaero.domain.user.exception.DuplicateEmailException
import dsmhackathon18.yesandaero.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val authService = AuthService(userRepository, passwordEncoder)

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
}
