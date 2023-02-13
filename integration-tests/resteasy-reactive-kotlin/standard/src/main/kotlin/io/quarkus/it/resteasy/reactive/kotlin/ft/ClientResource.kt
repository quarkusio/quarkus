package io.quarkus.it.resteasy.reactive.kotlin.ft

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("/ft/client")
class ClientResource {
    @Inject
    @RestClient
    private lateinit var client: HelloClient

    @GET
    suspend fun get() = client.hello()
}
