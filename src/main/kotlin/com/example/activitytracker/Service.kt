package com.example.activitytracker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
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

interface Authentificate{
    fun hashPassword(password: String): String
}


@Component
class AuthentificateImpl : Authentificate{
    override fun hashPassword(password: String): String{
        return password
    }
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


interface ActionService {
    fun addMouseAction(act: MouseAction) : Action
    fun addKeyboardAction(act: KeyboardAction) : Action
    fun addAppAction(act: AppAction) : Action

    fun saveAllActions(actions: List<Action>) : List<Action>

    fun getAllActionsFromUser(userId: Long): List<Action>
}



@Component
class ActionServiceImpl(
    var actionDao: ActionRepository,
) :
    ActionService {

    override fun addMouseAction(act: MouseAction) = actionDao.save(act)

    override fun addKeyboardAction(act: KeyboardAction) = actionDao.save(act)

    override fun addAppAction(act: AppAction) = actionDao.save(act)

    override fun saveAllActions(actions: List<Action>): List<Action> = actionDao.saveAll(actions).toList()

    override fun getAllActionsFromUser(userId: Long): List<Action> = actionDao.findAll().filter { it.id == userId }.toList()

}














































