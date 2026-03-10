package com.example.activitytracker.Services

import com.example.activitytracker.Authentificate
import com.example.activitytracker.CreateUserDto
import com.example.activitytracker.HelloError
import com.example.activitytracker.NotFoundError
import com.example.activitytracker.User
import com.example.activitytracker.UserRepository
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrElse


interface UserService {
    fun getUserById(id: Long): User?
    fun getAllUsers(): List<User>
    fun getUsersByName(name: String): List<String>
    fun createUser(user: CreateUserDto, password: String): User
    fun deleteUser(user: User)
    fun updateUser(user: User): User
}


@Component
class UserServiceImpl (
    var userDao : UserRepository,
    var authentificationEntity: Authentificate,
) :
    UserService {

    override fun getUserById(id: Long): User? = userDao.findById(id).getOrElse { throw NotFoundError("no user with id $id") }

    override fun getAllUsers(): List<User> = userDao.findAll().map { it ?: throw NotFoundError("findAllUsers failed in user service") }

    // Should be done with a query
    override fun getUsersByName(name: String): List<String> = userDao.findAll().filter { it.name.contains(name) }.map { it.name }.toList()

    override fun createUser(user: CreateUserDto, password: String): User = userDao.save(User(user.name, user.apply {
        if( role !in listOf("ROLE_USER", "ROLE_ADMIN")  ) {
            throw HelloError("invalid role: $role")
        }
    }.name, authentificationEntity.hashPassword(password)))

    override fun deleteUser(user: User) = userDao.deleteById(user.id)

    override fun updateUser(user: User): User = userDao.save(user)

}







