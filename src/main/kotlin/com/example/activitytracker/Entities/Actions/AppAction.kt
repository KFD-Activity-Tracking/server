package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "app_action")
@Entity
class AppAction() : Action() {
    @Column(name = "app_name")
    var app_name: String = ""
}