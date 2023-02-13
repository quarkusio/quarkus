package io.quarkus.it.resteasy.reactive.kotlin.ft

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.faulttolerance.Fallback
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/ft/hello")
@RegisterRestClient(configKey = "ft-hello")
interface HelloClient {
    @GET
    @Fallback(fallbackMethod = "fallback")
    suspend fun hello(): String

    suspend fun fallback() = "fallback"
}
