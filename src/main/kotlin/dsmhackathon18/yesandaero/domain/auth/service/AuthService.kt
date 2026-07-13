package dsmhackathon18.yesandaero.domain.auth.service

import dsmhackathon18.yesandaero.domain.auth.dto.SignupRequest
import dsmhackathon18.yesandaero.domain.auth.dto.SignupResponse
import dsmhackathon18.yesandaero.domain.user.entity.User
import dsmhackathon18.yesandaero.domain.user.exception.DuplicateEmailException
import dsmhackathon18.yesandaero.domain.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
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
}
