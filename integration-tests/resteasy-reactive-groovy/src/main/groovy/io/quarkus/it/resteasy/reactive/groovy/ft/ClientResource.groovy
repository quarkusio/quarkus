package io.quarkus.it.resteasy.reactive.groovy.ft

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("/ft/client")
class ClientResource {
    @Inject @RestClient private HelloClient client

    @GET
    def get() {
        client.hello()
    }
}
