package io.quarkus.it.resteasy.reactive.kotlin.ft

import java.util.concurrent.atomic.AtomicBoolean
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.POST
import javax.ws.rs.Path

@Path("/ft/hello")
class HelloResource {
    private val fail = AtomicBoolean(false)

    @GET
    fun get(): String {
        if (fail.get()) {
            throw InternalServerErrorException()
        }
        return "Hello, world!"
    }

    @POST
    @Path("/fail")
    fun startFailing() {
        fail.set(true)
    }

    @POST
    @Path("/heal")
    fun stopFailing() {
        fail.set(false)
    }
}
