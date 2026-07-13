package dsmhackathon18.yesandaero.domain.auth.controller

import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.auth.dto.SignupResponse
import dsmhackathon18.yesandaero.domain.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: SignupRequest): SignupResponse =
        authService.signup(request)
}
