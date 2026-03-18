package com.example.activitytracker

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/users")
class UserController(
    val userService: UserService,
){

    @GetMapping("/all")
    fun getAllUsers() = userService.getAllUsers();  //returns List<User>

    @PostMapping("/add")
    fun addUser(@RequestBody createUser: CreateUserDto) =
        userService.createUser(createUser, "qwerty123")

    @GetMapping("/findId/{userId}")
    fun findUserById(@PathVariable userId: Long) = userService.getUserById(userId)

}