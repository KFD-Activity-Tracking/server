package com.example.activitytracker

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table




@Entity
@Table(name = "users_")
class Users (
    var name: String,
    var role: String,
    var passwordHash: String,
) : IdCreatedAtBaseTable() {

    constructor(
        id: Long,
        name: String,
        role: String,
        passwordHash: String,
    ) : this(name, role, passwordHash) {
        this.id = id
    }

    constructor(
        id: Long,
    ) : this("", "", "") {
        this.id = id
    }

    @ManyToMany
    @JoinTable(
        name = "admin_lookup",
        joinColumns = [JoinColumn(name = "admin_id")],
        inverseJoinColumns = [JoinColumn(name = "subordinate_id")]
    )
    var subordinates : MutableList<Users> = mutableListOf()
}









data class DtoCreateUserRequest (
    val username: String,
    val role: String,
    val hashPassword : String,
)

data class DtoUpdateUserRequest (
    val id: Long,
    val username: String,
    val role: String,
    val hashPassword: String,
    val subordinates: List<Int>,
)

data class DtoUserInfoResponse(
    val userId: Long,
    val username: String,
    val role: String,
)

//not sure, since I specify user by username it technically serves as ID which is wrong
//
data class DtoAuthRequest(
    val username: String,
    val password: String,
    //for register only [ROLE_ADMIN, ROLE_USER, ROLE_MANAGER]
    val role: String = "",
)












































