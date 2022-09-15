package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.HttpHeaders
import org.jboss.resteasy.reactive.RestHeader

@Path("/greeting")
class GreetingResource(val headers: HttpHeaders) {

    @GET
    suspend fun testSuspend(@RestHeader("firstName") firstName: String): Greeting {
        val lastName = headers.getHeaderString("lastName")
        return Greeting("hello $firstName $lastName")
    }

    @GET
    @Path("noop")
    suspend fun noop() {
    }
}

data class Greeting(val message: String)
