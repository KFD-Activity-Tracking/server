package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.springframework.beans.factory.annotation.Value


@Entity
@Table(name = "users_")
class Users(

    id: Long = 0,

    @Column(name = "username")
    var username: String = "",

    @Column(name = "role")
    var role: String = "",

    @Column(name = "passwordHash")
    var passwordHash: String = "",


    @ManyToMany
    @JoinTable(
        name = "admin_lookup",
        joinColumns = [JoinColumn(name = "admin_id")],
        inverseJoinColumns = [JoinColumn(name = "subordinate_id")]
    )
    var subordinates: MutableList<Users> = mutableListOf(),

): IdCreatedAtBaseTable(id) {




    fun toDtoSimpleUserMap(): DtoSimpleUserMap {
        return DtoSimpleUserMap(
            this.id,
            this.username,
            this.role,
            this.subordinates.map { it.id }
        )
    }

}






interface ProjectionUser{
    fun getId() : Long
    fun getUsername() : String
    fun getRole() : String
    fun getPasswordHash() : String

    @Value("#{target.subordinates.![id]}")
    fun getSubordinates() : List<Long>


}


fun ProjectionUser.toDtoSimpleUserMap() : DtoSimpleUserMap{
    return DtoSimpleUserMap(
        id = getId(),
        username = getUsername(),
        role = getRole(),
        subordinates = getSubordinates(),
    )
}


data class DtoCreateUserRequest (
    val username: String,
    val role: String,
    val hashPassword : String,
)

data class DtoSimpleUserMap (
    val id: Long,
    val username: String,
    val role: String,
    val subordinates: List<Long>,
){
    fun toUserEntity(): Users{
        return Users(
            id = id,
            username = username,
            role = role,
            subordinates = subordinates.map { Users(id=it) }.toMutableList(),
        )
    }
}


//not sure, since I specify user by username it technically serves as ID which is wrong
//
data class DtoAuthRequest(
    val username: String,
    val password: String,
    //for register only [ADMIN, USER, MANAGER]
    val role: String = "",
)












































