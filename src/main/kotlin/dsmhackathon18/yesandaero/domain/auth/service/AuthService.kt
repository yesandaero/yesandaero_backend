package dsmhackathon18.yesandaero.domain.auth.service

import dsmhackathon18.yesandaero.domain.auth.dto.LoginRequest
import dsmhackathon18.yesandaero.domain.auth.dto.LoginResponse
import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.auth.dto.SignupResponse
import dsmhackathon18.yesandaero.domain.user.entity.User
import dsmhackathon18.yesandaero.domain.user.exception.DuplicateEmailException
import dsmhackathon18.yesandaero.domain.user.exception.LoginFailedException
import dsmhackathon18.yesandaero.domain.user.repository.UserRepository
import dsmhackathon18.yesandaero.global.jwt.JwtTokenProvider
import dsmhackathon18.yesandaero.global.jwt.RefreshTokenRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException()
        }

        val user = User(
            username = request.username,
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
            email = request.email,
            role = request.role,
        )
        val saved = userRepository.save(user)

        return SignupResponse(userId = requireNotNull(saved.id))
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email) ?: throw LoginFailedException()
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw LoginFailedException()
        }

        val userId = requireNotNull(user.id)
        val accessToken = jwtTokenProvider.generateAccessToken(userId, user.role)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userId)
        refreshTokenRepository.save(userId, refreshToken, jwtTokenProvider.refreshTokenTtl)

        return LoginResponse(accessToken = accessToken, refreshToken = refreshToken, role = user.role)
    }

    fun logout(userId: Long) {
        refreshTokenRepository.delete(userId)
    }
}
