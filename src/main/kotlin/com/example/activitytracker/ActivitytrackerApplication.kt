package com.example.activitytracker

import com.sun.jdi.ClassType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.LocalDateTime
import kotlin.reflect.KClass

@SpringBootApplication
@ConfigurationPropertiesScan
class ActivitytrackerApplication

fun main(args: Array<String>) {
	runApplication<ActivitytrackerApplication>(*args)
}


@ConfigurationProperties("what-to-answer-to-hw")
class WhatToAnswerToHw (
    val answer: String
)



open class SuperWebError(
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
) : SuperWebError(reason, HelloError::class.java, "SMth")


@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundError(
    val reason: String
) : SuperWebError(reason, NotFoundError::class.java,"")



@ControllerAdvice
class GlobalException : ResponseEntityExceptionHandler() {
    @ExceptionHandler(SuperWebError::class)
    fun handleError(e: SuperWebError): String {
        return e.toString()
    }
}
























