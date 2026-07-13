package dsmhackathon18.yesandaero.domain.auth.controller

import dsmhackathon18.yesandaero.domain.auth.dto.LoginRequest
import dsmhackathon18.yesandaero.domain.auth.dto.LoginResponse
import dsmhackathon18.yesandaero.domain.auth.dto.RefreshRequest
import dsmhackathon18.yesandaero.domain.auth.dto.RefreshResponse
import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.auth.dto.SignupResponse
import dsmhackathon18.yesandaero.domain.auth.service.AuthService
import dsmhackathon18.yesandaero.domain.user.entity.Role
import dsmhackathon18.yesandaero.domain.user.exception.DuplicateEmailException
import dsmhackathon18.yesandaero.domain.user.exception.LoginFailedException
import dsmhackathon18.yesandaero.global.exception.handler.GlobalExceptionHandler
import dsmhackathon18.yesandaero.global.jwt.exception.ExpiredOrInvalidTokenException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerTest {

    private val authService = mockk<AuthService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(AuthController(authService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `회원가입에 성공하면 201과 userId를 반환한다`() {
        every {
            authService.signup(SignupRequest("kimsiheun", "user@example.com", "P@ssw0rd!", Role.CUSTOMER))
        } returns SignupResponse(userId = 1L)

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"kimsiheun","email":"user@example.com","password":"P@ssw0rd!","role":"CUSTOMER"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(1))
    }

    @Test
    fun `이메일이 중복되면 409와 USR_409를 반환한다`() {
        every {
            authService.signup(SignupRequest("kimsiheun", "user@example.com", "P@ssw0rd!", Role.CUSTOMER))
        } throws DuplicateEmailException()

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"kimsiheun","email":"user@example.com","password":"P@ssw0rd!","role":"CUSTOMER"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("USR_409"))
    }

    @Test
    fun `요청 값이 비어있으면 400과 GLB_400을 반환한다`() {
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"","email":"not-an-email","password":"","role":"CUSTOMER"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLB_400"))
    }

    @Test
    fun `로그인에 성공하면 200과 토큰을 반환한다`() {
        every {
            authService.login(LoginRequest("user@example.com", "P@ssw0rd!"))
        } returns LoginResponse("access-token", "refresh-token", Role.CUSTOMER)

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"user@example.com","password":"P@ssw0rd!"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
    }

    @Test
    fun `이메일 또는 비밀번호가 일치하지 않으면 401과 USR_401을 반환한다`() {
        every {
            authService.login(LoginRequest("user@example.com", "wrong-password"))
        } throws LoginFailedException()

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"user@example.com","password":"wrong-password"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("USR_401"))
    }

    @Test
    fun `로그아웃에 성공하면 204를 반환한다`() {
        every { authService.logout(1L) } returns Unit
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            1L, null, listOf(SimpleGrantedAuthority("ROLE_CUSTOMER")),
        )

        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isNoContent)

        verify { authService.logout(1L) }
    }

    @Test
    fun `토큰 재발급에 성공하면 200과 새 토큰을 반환한다`() {
        every {
            authService.refresh(RefreshRequest("refresh-token"))
        } returns RefreshResponse("new-access-token", "new-refresh-token")

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"refresh-token"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
    }

    @Test
    fun `refreshToken이 만료되었거나 위조되면 401과 JWT_401을 반환한다`() {
        every {
            authService.refresh(RefreshRequest("invalid-token"))
        } throws ExpiredOrInvalidTokenException()

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"invalid-token"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("JWT_401"))
    }
}
