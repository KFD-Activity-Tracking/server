package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

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