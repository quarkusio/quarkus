package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.runtime.Quarkus.run
import io.quarkus.runtime.annotations.QuarkusMain

@QuarkusMain
class GreetingApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            run(*args)
        }
    }
}
