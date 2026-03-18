package com.example.activitytracker

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/actions")
class ActionController(
    val actionService: ActionService,
) {

    @PostMapping("/addAll")
    fun addActionList(@RequestBody acts : List<Action> ) = actionService.saveAllActions(acts)

    @GetMapping("/from/{userId}")
    fun getActionsOfUser(@PathVariable userId: Long) = actionService.getAllActionsFromUser(userId)

}
