package com.example.activitytracker

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.LocalDateTime
import kotlin.random.Random

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class ActivitytrackerApplication

fun main(args: Array<String>) {
	runApplication<ActivitytrackerApplication>(*args)
}


val <T> T.NOT_COMPLETED get(): T {
    val is_testing = true
    if (!is_testing){
        throw NotImplementedError("TODO!!!!")
    }
    return this
}


@Component
class StartupTasks (
    val userController: UserController,
    val userService: UserService,
    val actionService: ActionService,
    val statsService: StatisticsService,
) {

    var used = false

    @Scheduled(fixedDelay = 200)
    @Transactional
    fun once() {
        if (used){
            return
        }
        used = true
        println("RUNNING ONE-TIME TASKS")


        try {
            val ers = userController.addUser(DtoAuthRequest("user", "user", "USER"))
            println("ADDED USER ${ers.id}, ${ers.username}")
        } catch (e: Exception) {
            println("ERROR ADDING USER: : : :: $e ")
        }


        val ers = userService.getUserByName("user") ?: throw NotFoundException("User not found IN STARTUP")

        ers.realName = "Ivan Ivanov CanBeCyrillicIThink"
        userService.updateUser(ers)
        val random = Random(System.currentTimeMillis())

        actionService.saveAllActions(List<Action>(200, {
            MouseAction().also { it.user = Users(id=ers.id)
                it.delta_x = random.nextFloat()
                it.delta_y = random.nextFloat()
                it.is_click = random.nextBoolean()
            }
        }))
        statsService.collectAllStatistics()

        actionService.saveAllActions(List<Action>(200, {
            MouseAction().also { it.user = Users(id=ers.id)
                it.delta_x = random.nextFloat()
                it.delta_y = random.nextFloat()
                it.is_click = random.nextBoolean()
            }
        }))

        println("ADDED actions for  ${ers.id}, ${ers.username}")
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
























