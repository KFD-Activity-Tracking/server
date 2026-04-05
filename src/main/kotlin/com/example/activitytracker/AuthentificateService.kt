package com.example.activitytracker

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Service
import java.lang.System.currentTimeMillis
import java.util.Date

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.time.Duration.Companion.hours


@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtFilter: JwtFilter): SecurityFilterChain {
        http.csrf { it.disable() }
        http.authorizeHttpRequests {
                it.requestMatchers("/auth/**", "/frontend/**").permitAll()
                it.requestMatchers("/*.css", "/*.js").permitAll()
                it.anyRequest().authenticated()
            }


        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)


        return http.build()
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }



}




interface JwtService{
    fun generateJwt(username: String): String

    fun extractUsername(token: String): String

    fun validateToken(token: String): Boolean
}



@ConfigurationProperties("jwt-super-secret-key")
class JwtMySuperSecretKey (
    val key: String
)


@Service
class JwtServiceImpl(
    val jwtKey: JwtMySuperSecretKey,

) : JwtService {


    fun GetExspiration(): Date {
        val current = currentTimeMillis() + 24.hours.inWholeMilliseconds;
        return Date(current)
    }

    //is token expired
    fun isExpired(token: String): Boolean{
        val claim = io.jsonwebtoken.Jwts.parser().setSigningKey(jwtKey.key).parseClaimsJws(token).body
        return claim.expiration.before(Date())
    }

    override fun generateJwt(username: String): String {
        val builder = io.jsonwebtoken.Jwts.builder()

        val token = builder
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(GetExspiration())
            .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, jwtKey.key)
            .compact()

        return token
    }

    override fun extractUsername(token: String): String {
        val parse = io.jsonwebtoken.Jwts.parser()

        var res: String = ""
        try {
            res = parse.setSigningKey(jwtKey.key).parseClaimsJws(token).body.subject
        } catch (e: Exception) {
            throw e
        }

        return res
    }

    override fun validateToken(token: String): Boolean {
        val username = extractUsername(token)
        if (isExpired(token)) {
            return false
        }
        // etc
        return true
    }

}





@Service
class JwtFilter(
    val jwtService: JwtService,
    val userService: UserService,
    val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || authHeader.length < 8) {
            filterChain.doFilter(request, response)
            return
        }

        println("DEBUG authHeader = ${authHeader}")
        val token = authHeader.substring(7)

        val username = try {
            jwtService.extractUsername(token)
        } catch (e: Exception){
            println(e.message)

            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication == null) {

            val userDetails = userDetailsService.loadUserByUsername(username)

            val userPasswToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

            if (!jwtService.validateToken(token)){
                filterChain.doFilter(request, response)
                return
            }

            SecurityContextHolder.getContext().authentication = userPasswToken
        }

        filterChain.doFilter(request, response)


    }

}



data class DtoTokenResponse(
    val token: String
)








































