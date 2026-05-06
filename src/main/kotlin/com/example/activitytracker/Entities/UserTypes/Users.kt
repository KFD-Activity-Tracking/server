package com.example.activitytracker

import com.fasterxml.jackson.annotation.JsonIgnore
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


    @Column(name = "front_name")
    var realName: String = "",
    @Column(name = "screen_ratio")
    var screenRatio: Float = 1f,

    @Column(name = "role")
    var role: String = "",

    @Column(name = "passwordHash")
    var passwordHash: String = "",




    @JsonIgnore
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
            this.realName,
            this.role,
            this.subordinates.map { it.id },
            this.screenRatio
        )
    }

}






interface ProjectionUser{
    fun getId() : Long
    fun getUsername() : String
    fun getRole() : String
    fun getPasswordHash() : String
    fun getRealName() : String
    fun getScreenRatio() : Float

    @Value("#{target.subordinates.![id]}")
    fun getSubordinates() : List<Long>


}


fun ProjectionUser.toDtoSimpleUserMap() : DtoSimpleUserMap{
    return DtoSimpleUserMap(
        id = getId(),
        username = getUsername(),
        role = getRole(),
        subordinates = getSubordinates(),
        screenRatio = getScreenRatio(),
        realName = getRealName(),
    )
}


data class DtoCreateUserRequest (
    val username: String,
    val realName: String,
    val role: String,
    val hashPassword : String,
)

data class DtoSimpleUserMap (
    val id: Long,
    val username: String,
    var realName: String,
    val role: String,
    val subordinates: List<Long>,
    var screenRatio: Float,
){
    fun toUserEntity(): Users{
        return Users(
            id = id,
            username = username,
            realName = realName,
            role = role,
            subordinates = subordinates.map { Users(id=it) }.toMutableList(),
            screenRatio = screenRatio,
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












































