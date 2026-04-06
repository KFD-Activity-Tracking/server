package com.example.activitytracker

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrElse


interface UserService {
    fun getUserById(id: Long): Users?
    fun getAllUsers(): List<DtoSimpleUserMap>
    fun getUserByName(name: String): Users?
    fun createUser(user: DtoCreateUserRequest): Users
    fun deleteUser(user: Long)
    fun updateUser(user: DtoSimpleUserMap): Users


}


@Component
class UserServiceImpl (
    var userDao : UserRepository,
) :
    UserService {

    override fun getUserById(id: Long): Users? = userDao.findById(id).getOrElse { throw NotFoundException("no user with id $id") }

    override fun getAllUsers(): List<DtoSimpleUserMap> = userDao.findAll().map { it.toDtoSimpleUserMap() }


    override fun getUserByName(name: String): Users? = userDao.findByUsername(name) ?:
        throw NotFoundException("getUserByName failed in user service (reques = ${name})")



    override fun createUser(user: DtoCreateUserRequest): Users =
        userDao.save(
            Users(
                username =  user.username,
                role = getRole(user.role),
                passwordHash = user.hashPassword,))


    fun getRole(role: String) : String = role.apply{
        if( role !in listOf("USER", "ADMIN", "MANAGER")  ) {
            throw HelloException("invalid role: $role")
        }
    }


    //just id is enough
    override fun deleteUser(user: Long) = userDao.deleteById(user)

    override fun updateUser(user: DtoSimpleUserMap): Users {
        val user_old = userDao.findById(user.id).getOrElse { throw NotFoundException("user with id ${user.id} not found") }
        if (user_old.username!=user.username) {
            val user_same = userDao.findByUsername(user.username)
            if (user_same != null) {
                throw AlreadyExistsException("user with name ${user.username} already exists")
            }
        }

        return userDao.save(user) ?: throw HelloException("Unexpected error in userService update")
    }

}






@Service
class UserDetailsServiceImpl (
    var userService: UserService,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {

        val user = userService.getUserByName(username) ?: throw NotFoundException("user $username not found")

        val builder = org.springframework.security.core.userdetails.User.builder()
        val res = builder
            .username(user.username)
            .password(user.passwordHash)
            .roles(user.role)
            .build()

        return res
    }
}





























