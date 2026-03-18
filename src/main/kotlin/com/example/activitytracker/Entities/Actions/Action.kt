package com.example.activitytracker

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import java.time.LocalDateTime

//Base class for actions: mouse movement, app opened etc.
@Table(name = "action")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = MouseAction::class, name = "mouse"),
    JsonSubTypes.Type(value = KeyboardAction::class, name = "keyboard"),
    JsonSubTypes.Type(value = AppAction::class, name = "app")
)
open class Action() {

    @Column(name="id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0


    @Column(name="performed_at")
    open var performed_at : LocalDateTime = LocalDateTime.now()

    @Column(name="user_id")
    open var user_id : Long = 0

}