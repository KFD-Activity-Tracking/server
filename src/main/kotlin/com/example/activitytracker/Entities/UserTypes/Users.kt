package com.example.activitytracker

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "users_")
class Users (
    var name: String,
    var role: String,
    var passwordHash: String,
) : IdCreatedAtBaseTable() {
    @ManyToMany
    @JoinTable(
        name = "admin_lookup",
        joinColumns = [JoinColumn(name = "admin_id")],
        inverseJoinColumns = [JoinColumn(name = "subordinate_id")]
    )
    var subordinates : MutableList<Users> = mutableListOf()
}