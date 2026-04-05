package com.example.activitytracker

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/users")
class UserController(
    val userService: UserService,
    val passwordEncoder : PasswordEncoder,
    val jwtService: JwtService,
){


    @GetMapping("/owninfo")
    fun getCurrentUser(@RequestHeader("Authorization") authHeader: String) : DtoUserInfoResponse {
        val token = authHeader.substring(7)
        val user = userService.getUserByName(jwtService.extractUsername(token))!!
        val res = DtoUserInfoResponse(
            user.id,
            user.name,
            user.role,
        )
        return res
    }


    @GetMapping("/all")
    fun getAllUsers() = userService.getAllUsers();  //returns List<User>

    @PostMapping("/add")
    fun addUser(@RequestBody userAuth: DtoAuthRequest) {
        if (userService.getUserByName(userAuth.username) != null) {
            throw AlreadyExistsException("user ${userAuth.username} already exists")
        }

        val hashedPass = passwordEncoder.encode(userAuth.password);

        val createRq = DtoCreateUserRequest(userAuth.username, userAuth.role, hashedPass)

        userService.createUser(createRq)
    }


    @PostMapping("/delete/{userId}")
    fun deleteUser(@PathVariable userId: Long) {
        userService.deleteUser(Users(userId))
    }

    @GetMapping("/findId/{userId}")
    fun findUserById(@PathVariable userId: Long) : Users? = userService.getUserById(userId)




}