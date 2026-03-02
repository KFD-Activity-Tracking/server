package com.example.activitytracker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

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

@RestController
@RequestMapping("/hello")
class HelloW {
    @Autowired
    lateinit var data: WhatToAnswerToHw
    init{
        println("http://localhost:8765/hello/world")
    }
    @GetMapping("/world")
    fun helloworld() = data.answer
}



@ResponseStatus(HttpStatus.BAD_REQUEST)
class HelloError(
    val error: String
) : Exception()


@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundError(
    val error: String
) : Exception()



@ControllerAdvice
class GlobalException : ResponseEntityExceptionHandler() {
    @ExceptionHandler(HelloError::class)
    fun handleError(e: HelloError): String {
        return e.error
    }
}
























