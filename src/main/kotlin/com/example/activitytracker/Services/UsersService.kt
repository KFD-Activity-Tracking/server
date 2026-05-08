package com.example.activitytracker

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrElse


interface UserService {

    //read requests
    fun getUserById(id: Long): DtoSimpleUserMap?
    fun getAllUsers(callerId: Long, callerRole: String): List<DtoSimpleUserMap>
    fun getUserByName(name: String): DtoSimpleUserMap?
    fun getDetailedByName(username: String) : ProjectionUser?

    //write requests
    fun createUser(user: DtoCreateUserRequest) : DtoSimpleUserMap
    fun deleteUser(user: Long)
    fun updateUser(user: DtoSimpleUserMap) : DtoSimpleUserMap
    fun addSubordinate(managerId: Long, subordinateId: Long)
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

    override fun getAllUsers(callerId: Long, callerRole: String): List<DtoSimpleUserMap> {
        if (callerRole == "ADMIN") return userDao.findAll().map { it.toDtoSimpleUserMap() }
        val manager = userDao.findById(callerId).orElse(null) ?: return emptyList()
        return userDao.findAllById(manager.subordinates.map { it.id }).map { it.toDtoSimpleUserMap() }
    }


    override fun getUserByName(name: String): DtoSimpleUserMap? =
        (userDao.findProjectionByUsername(name))?.toDtoSimpleUserMap()

    override fun getDetailedByName(username: String): ProjectionUser? = userDao.findProjectionByUsername(username)

    override fun createUser(user: DtoCreateUserRequest) : DtoSimpleUserMap {
        val toCreate = Users(
            username = user.username,
            role = getRole(user.role),
            passwordHash = user.hashPassword,
            realName = user.realName,
        )
        val userID = userDao.save(toCreate).id

        return userDao.findProjectionById(userID)?.toDtoSimpleUserMap()
            ?: throw HelloException("unexpected error in UserService.createUser")
    }


    fun getRole(role: String) : String = role.apply{
        if( role !in listOf("USER", "ADMIN", "MANAGER")  ) {
            throw HelloException("invalid role: $role")
        }
    }


    @org.springframework.transaction.annotation.Transactional
    override fun addSubordinate(managerId: Long, subordinateId: Long) {
        val manager = userDao.findById(managerId).orElse(null)
            ?: throw NotFoundException("Manager $managerId not found")
        val sub = userDao.findById(subordinateId).orElse(null)
            ?: throw NotFoundException("User $subordinateId not found")
        if (manager.subordinates.none { it.id == subordinateId }) {
            manager.subordinates.add(sub)
            userDao.save(manager)
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

        val user_new = user.toUserEntity().also { it.passwordHash = user_old.passwordHash }

        val userId = userDao.save(user_new).id

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





























