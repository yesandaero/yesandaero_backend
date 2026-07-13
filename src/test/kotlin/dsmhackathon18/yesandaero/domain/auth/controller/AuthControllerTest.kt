package dsmhackathon18.yesandaero.domain.auth.controller

import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.auth.dto.SignupResponse
import dsmhackathon18.yesandaero.domain.auth.service.AuthService
import dsmhackathon18.yesandaero.domain.user.entity.Role
import dsmhackathon18.yesandaero.domain.user.exception.DuplicateEmailException
import dsmhackathon18.yesandaero.global.exception.handler.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
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
            .build()
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
}
