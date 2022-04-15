package io.quarkus.it.resteasy.reactive.kotlin

import org.jboss.resteasy.reactive.RestHeader
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.HttpHeaders

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

data class Greeting(val message:String)
