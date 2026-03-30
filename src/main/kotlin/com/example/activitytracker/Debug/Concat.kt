package com.example.activitytracker.Debug


import com.example.activitytracker.Debug.BlacklistConcat.blacklist
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.time.Duration.Companion.days


object BlacklistConcat{
    val blacklist = "server\\build, ".parsef
}
fun allFiles() : List<Path>{

    val pth = Paths.get("")

    val res = mutableListOf<Path>()
    val stck = mutableListOf<Path>()
    stck.add(pth)

    while (stck.isNotEmpty()) {
        val next = stck.removeLast()
        if (blacklist.any { next.toAbsolutePath().toString().contains(it) }) {
            continue
        }
        if (Files.isDirectory(next)) {
            for (i in Files.list(next)) {
                stck.add(i)
            }
        } else {
            res.add(next)
        }
    }

    return res
}


public val String.parsef get() = this.split(' ', ',').filter { it.length>2 }.toList()
fun Path.readf() = "*".repeat(70) + "\nFile " + this.toString() + ":\n" + "*".repeat(70) +
        "\n".repeat(2) + Files.readString(this)



class ConcatTest {
    @Test
    fun ExternalO(){
        //getActions()
        getControllers()
        getProperties()
        getServices()
        getEntities()
        printAll("Users.kt")
    }

    fun getActions(){
        printAll("Action")
    }

    fun getEntities(){
        printAll("Entity")
    }

    fun getControllers(){
        printAll("Controller")
    }

    fun getServices(){
        printAll("Service")
    }

    fun getProperties(){
        printAll("build.gradle.kts, properties")
    }



    fun printAll(wc: String){
        wc.parsef.forEach { println(getFiles(it).joinToString("\n")) }
    }

}


fun getFiles(wc: String) : List<String>{


    val res = mutableListOf<String>()

    for (i in allFiles()){
        if ((i.name+"."+i.extension).contains(wc)){
            res.add(i.readf())
        }
    }

    return res
}























































