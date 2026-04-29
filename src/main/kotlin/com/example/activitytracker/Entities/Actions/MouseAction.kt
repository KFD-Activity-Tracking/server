package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "mouse_action")
@Entity
class MouseAction() : Action() {
    @Column(name = "delta_x")
    var delta_x: Float = 0f
    @Column(name = "delta_y")
    var delta_y: Float = 0f
    @Column(name = "is_click")
    var is_click: Boolean = false
}