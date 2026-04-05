package com.example.activitytracker

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrElse


interface UserService {
    fun getUserById(id: Long): Users?
    fun getAllUsers(): List<Users>
    fun getUserByName(name: String): Users?
    fun createUser(user: DtoCreateUserRequest): Users
    fun deleteUser(user: Users)
    fun updateUser(user: DtoUpdateUserRequest): Users


}


@Component
class UserServiceImpl (
    var userDao : UserRepository,
) :
    UserService {

    override fun getUserById(id: Long): Users? = userDao.findById(id).getOrElse { throw NotFoundException("no user with id $id") }

    override fun getAllUsers(): List<Users> = userDao.findAll().map { it ?: throw NotFoundException("findAllUsers failed in user service") }


    override fun getUserByName(name: String): Users? = userDao.findByName(name) ?: throw NotFoundException("getUserByName failed in user service")



    override fun createUser(user: DtoCreateUserRequest): Users =
        userDao.save(
            Users(
                user.username,
                getRole(user.role),
                user.hashPassword,))


    fun getRole(role: String) : String = role.apply{
        if( role !in listOf("USER", "ADMIN", "MANAGER")  ) {
            throw HelloError("invalid role: $role")
        }
    }


    //just id is enough
    override fun deleteUser(user: Users) = userDao.deleteById(user.id)

    override fun updateUser(user: DtoUpdateUserRequest): Users = userDao.save(Users(
        user.id,
        user.username,
        user.role,
        user.hashPassword,
    ))

}






@Service
class UserDetailsServiceImpl (
    var userService: UserService,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {

        val user = userService.getUserByName(username) ?: throw NotFoundException("user $username not found")

        val builder = org.springframework.security.core.userdetails.User.builder()
        val res = builder
            .username(user.name)
            .password(user.passwordHash)
            .roles(user.role)
            .build()

        return res
    }
}





























