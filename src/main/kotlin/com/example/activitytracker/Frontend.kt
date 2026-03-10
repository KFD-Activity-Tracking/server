package com.example.activitytracker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name



@ConfigurationProperties("what-to-answer-to-hw")
class WhatToAnswerToHw (
    val answer: String
)


@RestController
@RequestMapping("/hello")
class HelloW {
    @Autowired
    lateinit var data: WhatToAnswerToHw
    init{
        println("http://localhost:8765/hello/world")
    }
    @GetMapping("/world")
    fun helloworld() = data.answer
}


@Controller
@RequestMapping("/frontend")
class HttpFrontend{

    init {
        println("http://localhost:8765/frontend/deepseektest")
        println("http://localhost:8765/frontend/paths")
    }

    @GetMapping("/deepseektest")
    fun deepseekTest() : String{
        return "deepseekFrontend"
    }

    @ResponseBody
    @GetMapping("/paths")
    fun getPaths() : String{
        val pth = Paths.get("")
        val result = mutableListOf<String>()
        dfsDirectory(result, pth, 0)

        val str = "<html><body><pre>" +
                result.joinToString("\n") +
                "</pre></body></html>"


        return str
    }

    fun dfsDirectory(res : MutableList<String>, pth: Path, depth: Int){
        if ( depth>0 && (pth.name.isEmpty() || pth.name=="build" || pth.name[0]=='.')){
            return
        }
        val dist = " ".repeat(depth*2)
        if (Files.isDirectory(pth)){
            res.add(dist + ("/"+pth.name))
            Files.list(pth).forEach { dfsDirectory(res, it, depth+1) }
        } else{
            res.add(dist + pth.name)
        }
    }



}











