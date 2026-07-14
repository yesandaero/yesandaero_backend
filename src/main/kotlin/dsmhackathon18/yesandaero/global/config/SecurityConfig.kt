package dsmhackathon18.yesandaero.global.config

import dsmhackathon18.yesandaero.domain.user.exception.RoleNotAllowedException
import dsmhackathon18.yesandaero.global.jwt.JwtAuthenticationFilter
import dsmhackathon18.yesandaero.global.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    @Qualifier("handlerExceptionResolver")
    private val handlerExceptionResolver: HandlerExceptionResolver,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/signup", "/auth/login", "/auth/refresh").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider, handlerExceptionResolver),
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:8086")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { request, response, authException ->
            handlerExceptionResolver.resolveException(request, response, null, authException)
        }

    private fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { request, response, _ ->
            handlerExceptionResolver.resolveException(request, response, null, RoleNotAllowedException())
        }
}
