package com.example.activitytracker

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrElse


interface UserService {

    //read requests
    fun getUserById(id: Long): DtoSimpleUserMap?
    fun getAllUsers(): List<DtoSimpleUserMap>
    fun getUserByName(name: String): DtoSimpleUserMap?
    fun getDetailedByName(username: String) : ProjectionUser?

    //write requests
    fun createUser(user: DtoCreateUserRequest) : DtoSimpleUserMap
    fun deleteUser(user: Long)
    fun updateUser(user: DtoSimpleUserMap) : DtoSimpleUserMap
}


interface UsersProjection{

}

@Component
class UserServiceImpl (
    var userDao : UserRepository,
) :
    UserService {

    override fun getUserById(id: Long): DtoSimpleUserMap? =
        userDao.findById(id)
            .getOrElse { throw NotFoundException("no user with id $id") }
            .toDtoSimpleUserMap()

    override fun getAllUsers(): List<DtoSimpleUserMap> =
        userDao.findAll().map { it.toDtoSimpleUserMap() }


    override fun getUserByName(name: String): DtoSimpleUserMap? =
        (userDao.findProjectionByUsername(name) ?: throw NotFoundException("getUserByName failed in user service (reques = ${name})"))
            .toDtoSimpleUserMap()

    override fun getDetailedByName(username: String): ProjectionUser? = userDao.findProjectionByUsername(username)


    override fun createUser(user: DtoCreateUserRequest) : DtoSimpleUserMap {
        val userID = userDao.save(
            Users(
                username = user.username,
                role = getRole(user.role),
                passwordHash = user.hashPassword,
            )
        ).id.NOT_COMPLETED  // I think userDao.save loads Users.subordinates too, so if the graph is dense it loads the whole DB

        return userDao.findProjectionById(userID)?.toDtoSimpleUserMap()
            ?: throw HelloException("unexpected error in UserService.createUser")
    }


    fun getRole(role: String) : String = role.apply{
        if( role !in listOf("USER", "ADMIN", "MANAGER")  ) {
            throw HelloException("invalid role: $role")
        }
    }


    //just id is enough
    override fun deleteUser(user: Long) = userDao.deleteById(user)

    override fun updateUser(user: DtoSimpleUserMap) : DtoSimpleUserMap {
        val user_old = userDao.findById(user.id).getOrElse { throw NotFoundException("user with id ${user.id} not found") }
        if (user_old.username!=user.username) {
            val user_same = userDao.findProjectionByUsername(user.username)
            if (user_same != null) {
                throw AlreadyExistsException("user with name ${user.username} already exists")
            }
        }

        val userId = userDao.save(user.toUserEntity()).id.NOT_COMPLETED //same

        return userDao.findProjectionById(userId)?.toDtoSimpleUserMap()
            ?: throw HelloException("unexpected user with id ${user.id} not found")
    }

}






@Service
class UserDetailsServiceImpl (
    var userService: UserService,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {

        val user = userService.getDetailedByName(username) ?: throw NotFoundException("user $username not found")

        val builder = org.springframework.security.core.userdetails.User.builder()
        val res = builder
            .username(user.getUsername())
            .password(user.getPasswordHash())
            .roles(user.getRole())
            .build()

        return res
    }
}





























