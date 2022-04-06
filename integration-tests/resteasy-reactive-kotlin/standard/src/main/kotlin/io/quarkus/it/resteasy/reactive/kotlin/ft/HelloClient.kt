package io.quarkus.it.resteasy.reactive.kotlin.ft

import org.eclipse.microprofile.faulttolerance.Fallback
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/ft/hello")
@RegisterRestClient(configKey = "ft-hello")
interface HelloClient {
    @GET
    @Fallback(fallbackMethod = "fallback")
    suspend fun hello(): String

    suspend fun fallback() = "fallback"
}
