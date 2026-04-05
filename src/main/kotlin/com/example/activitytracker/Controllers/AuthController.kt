package com.example.activitytracker.Controllers

import com.example.activitytracker.AlreadyExistsException
import com.example.activitytracker.BadCredentialsException
import com.example.activitytracker.DtoAuthRequest
import com.example.activitytracker.DtoCreateUserRequest
import com.example.activitytracker.DtoTokenResponse
import com.example.activitytracker.JwtService
import com.example.activitytracker.NotFoundException
import com.example.activitytracker.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController



interface AuthController {

    fun login (@RequestBody request: DtoAuthRequest) : DtoTokenResponse

    fun register (@RequestBody request: DtoAuthRequest) : DtoTokenResponse

}

@RestController
@RequestMapping("/auth")
class AuthControllerImpl (

    val userService: UserService,
    val passwordEncoder: PasswordEncoder,
    val jwtService: JwtService,

    ) : AuthController {



    @PostMapping("/test-json")
    fun testJson(@RequestBody body: Map<String, String>): Map<String, String> {
        println("DEBUG Received JSON: $body")
        return mapOf("status" to "success", "received" to body.toString())
    }


    @PostMapping("/login")
    override fun login (@RequestBody request: DtoAuthRequest) : DtoTokenResponse {
        val user = userService.getUserByName(request.username)!!

        if (!passwordEncoder.matches(request.password, user.passwordHash)){
            throw BadCredentialsException("Wrong credentials for user ${request.username}")
        }

        val token = jwtService.generateJwt(user.name)

        return DtoTokenResponse(token)

    }



    @PostMapping("/register")
    override fun register(@RequestBody request: DtoAuthRequest): DtoTokenResponse {
        try {
            val user = userService.getUserByName(request.username)
            if (user != null){
                throw AlreadyExistsException("User with name ${request.username} already exists")
            }
        } catch (e: NotFoundException) { }


        val user = userService.createUser(DtoCreateUserRequest(
            username = request.username,
            request.role,
            passwordEncoder.encode(request.password),
        ))

        val token = jwtService.generateJwt(user.name)

        return DtoTokenResponse(token)

    }


}





































