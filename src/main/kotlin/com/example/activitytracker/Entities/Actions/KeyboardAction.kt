package com.example.activitytracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "keyboard_action")
@Entity
class KeyboardAction() : Action() {
    @Column(name = "keyboard_key")
    var keyboard_key: Int = -1
}