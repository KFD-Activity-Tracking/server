package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "mouse_action")
@Entity
class MouseAction() : Action() {
    @Column(name = "delta_x")
    var delta_x: Int = 0
    @Column(name = "delta_y")
    var delta_y: Int = 0
}