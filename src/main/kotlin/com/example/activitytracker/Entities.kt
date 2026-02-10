package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import java.time.LocalDateTime

@MappedSuperclass
open class IdCreatedAtBaseTable{
    @Column(name="id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id : Long = 0

    @Column(name="created_at")
    var createdAt : LocalDateTime = LocalDateTime.now()

}

@Entity
@Table(name = "user_")
class User (
    name: String,
    role: String,
) : IdCreatedAtBaseTable() {
    @ManyToMany
    @JoinTable(
        name = "admin_lookup",
        joinColumns = [JoinColumn(name = "admin_id")],
        inverseJoinColumns = [JoinColumn(name = "subordinate_id")]
    )
    var subordinates : MutableList<User> = mutableListOf()
}



// Class responsible for storing statistic over some period of time
@Entity
@Table(name = "statistics")
class Statistics(

) {

    @Column(name="id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name="start_time")
    var start_time: LocalDateTime = LocalDateTime.now()

    @Column(name="end_time")
    var end_time: LocalDateTime = LocalDateTime.now()

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user_id : User? = null

    //Main statistic data: \/




}




@Entity
@Table(name = "app_statistics")
class AppStatistics (){
    @Column(name="id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id : Long = 0

    @ManyToOne
    @JoinColumn(name = "statistics_id")
    var statistics_id : Statistics? = null

    @Column
    var app_name : String = ""

    @Column
    var time_spent: Long = 0

    @Column
    var number_of_exits : Int = 0

}






//Base class for actions: mouse movement, app opened etc.
@Table(name = "action")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
open class Action() {

    @Column(name="id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0


    @Column(name="performed_at")
    var performed_at : LocalDateTime = LocalDateTime.now()

}


@Table(name = "mouse_action")
@Entity
class MouseAction() : Action() {
    @Column(name = "delta_x")
    var delta_x: Int = 0
    @Column(name = "delta_y")
    var delta_y: Int = 0
}



@Table(name = "keyboard_action")
@Entity
class KeyboardAction() : Action() {
    @Column(name = "keyboard_key")
    var keyboard_key: Int = -1
}




@Table(name = "app_action")
@Entity
class AppAction() : Action() {
    @Column(name = "app_name")
    var app_name: String = ""
}





























