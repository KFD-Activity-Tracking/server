package com.example.activitytracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Duration
import java.time.LocalDateTime

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class ActivitytrackerApplication

fun main(args: Array<String>) {
	runApplication<ActivitytrackerApplication>(*args)
}



@Configuration
class AppConfig {


    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(90))
            .build()
    }
}

@Component
class StartupTasks(
    val userService: UserService,
    val passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder,
) {

    var used = false

    @Scheduled(fixedDelay = 200)
    @Transactional
    fun once() {
        if (used) return
        used = true

        try {
            userService.createUser(DtoCreateUserRequest(
                username = "user",
                realName = "Ivan Ivanov CanBeCyrillicIThink",
                role = "USER",
                hashPassword = passwordEncoder.encode("user")
            ))
        } catch (_: Exception) { }
    }

}

open class SuperWebException(
    val error: String,
    val type: Class<*>,
    val request: String,
) : Exception()
{
    val status = mutableMapOf<String, String>()
    init {
        status["errorText"] = error
        status["request"] = request
        status["time"] = LocalDateTime.now().toString()
        status["type"] = type.toString()
    }


}




@ResponseStatus(HttpStatus.BAD_REQUEST)
class HelloException(
    val reason: String
) : SuperWebException(reason, HelloException::class.java, "SMth")


@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(
    val reason: String
) : SuperWebException(reason, NotFoundException::class.java,"")


@ResponseStatus(HttpStatus.CONFLICT)
class AlreadyExistsException(
    val reason: String
) : SuperWebException(reason, AlreadyExistsException::class.java, "")


@ResponseStatus(HttpStatus.LOCKED)
class BadCredentialsException(
    val reason: String
) : SuperWebException(reason, BadCredentialsException::class.java, "")

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(
    val reason: String
) : SuperWebException(reason, UnauthorizedException::class.java, "")


@ControllerAdvice
class GlobalException : ResponseEntityExceptionHandler() {
    @ExceptionHandler(SuperWebException::class)
    fun handleError(e: SuperWebException): ResponseEntity<Map<String, String>> {

        val annotation = e::class.java.getAnnotation(ResponseStatus::class.java)
        val httpStatus = annotation.value

        return ResponseEntity
            .status(httpStatus)
            .body(e.status)

    }
}
























