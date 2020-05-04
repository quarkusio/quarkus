package com.example.quarkusmm

import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication


object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        Quarkus.run(MyApp::class.java, *args)
    }

    class MyApp : QuarkusApplication {
        @Throws(Exception::class)
        override fun run(vararg args: String): Int {
            println("Do startup logic here")
            Quarkus.waitForExit()
            return 0
        }
    }
}