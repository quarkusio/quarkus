package io.quarkus.it.resteasy.reactive.kotlin.ft

import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path

@Path("/ft/client")
class ClientResource {
    @Inject
    @RestClient
    private lateinit var client: HelloClient

    @GET
    suspend fun get() = client.hello()
}
