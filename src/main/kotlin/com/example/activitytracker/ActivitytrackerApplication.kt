package com.example.activitytracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
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
class ActivitytrackerApplication

fun main(args: Array<String>) {
	runApplication<ActivitytrackerApplication>(*args)
}







@RestController
@RequestMapping("/hello")
class helloW {
    @GetMapping("/world")
    fun helloworld() = "Hello world!"
}



@ResponseStatus(HttpStatus.BAD_REQUEST)
class HelloError(
    val error: String
) : Exception()




@ControllerAdvice
class GlobalException : ResponseEntityExceptionHandler() {
    @ExceptionHandler(HelloError::class)
    fun handleError(e: HelloError): String {
        return e.error
    }
}
























