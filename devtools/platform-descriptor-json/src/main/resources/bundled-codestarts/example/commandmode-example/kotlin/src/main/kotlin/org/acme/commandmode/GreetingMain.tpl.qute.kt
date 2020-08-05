package org.acme.commandmode

import javax.inject.Inject

import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain

@QuarkusMain
class GreetingMain: QuarkusApplication {

    @Inject
    lateinit var service: GreetingService
    override fun run(vararg args: String?): Int {
        if (args.isNotEmpty()) {
            System.out.println(service.greeting(args.joinToString(",")))
        } else {
            System.out.println(service.greeting("{greeting.default-name}"))
        }
        return 0
    }

}
