package com.example.activitytracker

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.LocalDateTime

@SpringBootApplication
@ConfigurationPropertiesScan
class ActivitytrackerApplication

fun main(args: Array<String>) {
	runApplication<ActivitytrackerApplication>(*args)
}



@Component
class StartupTasks (
    val userController: UserController,
    val userService: UserService,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        try {
            val defaultUser = userService.getUserByName("user")
            if (defaultUser != null) {
                userService.deleteUser(defaultUser)
            }
        } catch (e: Exception) {
            println(e.message)
        }
        try {
            userController.addUser(DtoAuthRequest("user", "user", "USER"))
        } catch (e: Exception) {
            println(e.message)
        }
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
class HelloError(
    val reason: String
) : SuperWebException(reason, HelloError::class.java, "SMth")


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
            .body(e.status,)

    }
}
























