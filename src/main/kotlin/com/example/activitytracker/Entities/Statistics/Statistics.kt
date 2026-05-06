package com.example.activitytracker

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

// Class responsible for storing statistic over some period of time

@MappedSuperclass
class BasicStatistics (

){

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

}




//Main statistic data: \/

@Entity
@Table(name = "statistics")
class Statistics : BasicStatistics() {



    @JsonManagedReference
    @OneToMany(mappedBy = "statistics_id", cascade = [(CascadeType.ALL)], orphanRemoval = true)
    var app_statistics : MutableList<AppStatistics> = mutableListOf()



    var date : LocalDateTime = LocalDateTime.now()



    var active_time: Long = 0



    var keyboard_to_mouse_coef: Float = 1f



    var mouse_movement: Long = 0
    var mouse_clicks: Int = 0
    var keyboard_clicks: Int = 0



    var idle_time: Long = 0



    var average_cpu: Double = 0.0
    var average_gpu: Double = 0.0
    var average_ram: Double = 0.0



    var login_time: Long = 0
    var logout_time: Long = 0


    // string represents x*y heatmap, each byte describes the density of clicks (per minute) in the cell's area
    // 100*100 by default
    @Lob
    var heat_map: String = ""
    var heat_map_width: Int = 0
    @Lob
    var clicks_over_time: String = ""



    @Lob
    var ai_eval: String = "no evaluation"

    var number_of_breaks: Int = 0


}






































