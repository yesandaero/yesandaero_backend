package dsmhackathon18.yesandaero.global.jwt

import dsmhackathon18.yesandaero.domain.user.entity.Role
import dsmhackathon18.yesandaero.global.jwt.exception.ExpiredOrInvalidTokenException
import dsmhackathon18.yesandaero.global.jwt.exception.MalformedTokenException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    jwtProperties: JwtProperties,
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    private val accessTokenExpiration = jwtProperties.accessTokenExpiration
    private val refreshTokenExpiration = jwtProperties.refreshTokenExpiration

    val refreshTokenTtl: Duration = Duration.ofMillis(refreshTokenExpiration)

    fun generateAccessToken(userId: Long, role: Role): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role.name)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpiration))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenExpiration))
            .signWith(key)
            .compact()
    }

    fun getUserId(token: String): Long =
        parseClaims(token).subject.toLong()

    fun getRole(token: String): Role {
        val role = parseClaims(token)[CLAIM_ROLE, String::class.java]
            ?: throw MalformedTokenException()
        return Role.valueOf(role)
    }

    fun parseClaims(token: String): Claims {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw ExpiredOrInvalidTokenException()
        } catch (e: MalformedJwtException) {
            throw MalformedTokenException()
        } catch (e: JwtException) {
            throw ExpiredOrInvalidTokenException()
        } catch (e: IllegalArgumentException) {
            throw MalformedTokenException()
        }
    }

    companion object {
        private const val CLAIM_ROLE = "role"
    }
}
