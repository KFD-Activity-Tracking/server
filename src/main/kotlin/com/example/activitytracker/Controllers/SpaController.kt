package com.example.activitytracker

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

// Forwards all non-API, non-static routes to the React SPA entry point
@Controller
class SpaController {

    @GetMapping(value = ["/{path:[^\\.]*}", "/{path:[^\\.]*}/**"])
    fun forward(): String = "forward:/index.html"
}