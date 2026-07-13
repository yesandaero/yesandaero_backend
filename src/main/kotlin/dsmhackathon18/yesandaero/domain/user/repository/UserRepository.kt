package dsmhackathon18.yesandaero.domain.user.repository

import dsmhackathon18.yesandaero.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): User?
}
