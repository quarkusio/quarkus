package io.quarkus.it.kotser

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Produces(APPLICATION_JSON)
abstract class BaseResource {

    @GET
    suspend fun list(): List<String> {
        delay(1.milliseconds)
        return listOf("a, b")
    }
}

@Path("/foo") @ApplicationScoped class FooResource : BaseResource()

@Path("/bar") @ApplicationScoped class BarResource : BaseResource()
