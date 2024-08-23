package org.acme

import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain

@QuarkusMain
class Main : QuarkusApplication {

    @Throws(Exception::class)
    override fun run(vararg args: String?): Int {
        Quarkus.waitForExit()
        return 0
    }
}
