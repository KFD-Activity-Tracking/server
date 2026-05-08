package com.example.activitytracker

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


    // http://localhost:8765/api/users/owninfo
    @GetMapping("/owninfo")
    fun getCurrentUser(@RequestHeader("Authorization") authHeader: String) = jwtService.extractUserFromHeader(authHeader)


    @PostMapping("/postOwnRatio")
    fun updateOwnScreenRatio(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody screenRatio: Float ) {
        val usr = jwtService.extractUserFromHeader(authHeader)
        usr.screenRatio = screenRatio
        userService.updateUser(usr)
    }


    @GetMapping("/all")
    fun getAllUsers(@RequestHeader("Authorization") authHeader: String): List<DtoSimpleUserMap> {
        val caller = jwtService.extractUserFromHeader(authHeader)
        return userService.getAllUsers(caller.id, caller.role)
    }

    @PostMapping("/add")
    fun addUser(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody userAuth: DtoAuthRequest,
    ): DtoSimpleUserMap {
        val caller = jwtService.extractUserFromHeader(authHeader)
        if (userService.getUserByName(userAuth.username) != null)
            throw AlreadyExistsException("user ${userAuth.username} already exists")

        val realName = userAuth.realName.ifBlank { userAuth.username }
        val created = userService.createUser(
            DtoCreateUserRequest(userAuth.username, realName, userAuth.role, passwordEncoder.encode(userAuth.password))
        )

        val assignTo = if (caller.role == "MANAGER") caller.id else userAuth.managerId
        if (assignTo != null) userService.addSubordinate(assignTo, created.id)

        return created
    }


    @PostMapping("/delete/{userId}")
    fun deleteUser(@PathVariable userId: Long) {
        userService.deleteUser(userId)
    }

    @PostMapping("/update")
    fun updateUser(@RequestBody user: DtoSimpleUserMap) : DtoSimpleUserMap = userService.updateUser(user)


    @GetMapping("/findId/{userId}")
    fun findUserById(@PathVariable userId: Long) : DtoSimpleUserMap? = userService.getUserById(userId)




}