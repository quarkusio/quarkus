package io.quarkus.it.resteasy.reactive.kotlin.ft

import jakarta.ws.rs.GET
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import java.util.concurrent.atomic.AtomicBoolean

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
