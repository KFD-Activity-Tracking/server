package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

// Class responsible for storing statistic over some period of time
@Entity
@Table(name = "statistics")
class Statistics(

)
{

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
    var user_id : Users? = null

    //Main statistic data: \/




}