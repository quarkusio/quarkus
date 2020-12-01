package org.acme

import javax.inject.Inject

import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain

@QuarkusMain
class {main.class-name}: QuarkusApplication {

    override fun run(vararg args: String?): Int {
        val name =  if (args.isNotEmpty()) args.joinToString(",") else "{greeting.default-name}"
        println(name)
        return 0
    }

}
