package com.example.activitytracker

import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrElse


interface UserService {
    fun getUserById(id: Long): Users?
    fun getAllUsers(): List<Users>
    fun getUsersByName(name: String): List<String>
    fun createUser(user: CreateUserDto, password: String): Users
    fun deleteUser(user: Users)
    fun updateUser(user: Users): Users
}


@Component
class UserServiceImpl (
    var userDao : UserRepository,
    var authentificationEntity: Authentificate,
) :
    UserService {

    override fun getUserById(id: Long): Users? = userDao.findById(id).getOrElse { throw NotFoundError("no user with id $id") }

    override fun getAllUsers(): List<Users> = userDao.findAll().map { it ?: throw NotFoundError("findAllUsers failed in user service") }

    // Should be done with a query
    override fun getUsersByName(name: String): List<String> = userDao.findAll().filter { it.name.contains(name) }.map { it.name }.toList()

    override fun createUser(user: CreateUserDto, password: String): Users = userDao.save(Users(user.name, user.apply {
        if( role !in listOf("ROLE_USER", "ROLE_ADMIN")  ) {
            throw HelloError("invalid role: $role")
        }
    }.name, authentificationEntity.hashPassword(password)))

    override fun deleteUser(user: Users) = userDao.deleteById(user.id)

    override fun updateUser(user: Users): Users = userDao.save(user)

}