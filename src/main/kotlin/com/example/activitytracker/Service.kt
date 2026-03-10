package com.example.activitytracker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrElse


interface Authentificate{
    fun hashPassword(password: String): String
}


@Component
class AuthentificateImpl : Authentificate{
    override fun hashPassword(password: String): String{
        return password
    }
}




























