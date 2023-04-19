package io.quarkus.it.resteasy.reactive.groovy.ft

import groovy.transform.CompileStatic
import jakarta.ws.rs.GET
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path

import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
@Path("/ft/hello")
class HelloResource {
    private final AtomicBoolean fail = new AtomicBoolean(false)

    @GET
    String get() {
        if (fail.get()) {
            throw new InternalServerErrorException()
        }
        'Hello, world!'
    }

    @POST
    @Path("/fail")
    void startFailing() {
        fail.set(true)
    }

    @POST
    @Path("/heal")
    void stopFailing() {
        fail.set(false)
    }
}
