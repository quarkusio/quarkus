package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import java.util.concurrent.atomic.AtomicReference
import org.jboss.resteasy.reactive.RestHeader

@Path("/greeting")
class GreetingResource(val headers: HttpHeaders) {

    companion object {
        val MY_PROPERTY = AtomicReference("unset")
    }

    @GET
    suspend fun testSuspend(@RestHeader("firstName") firstName: String): Greeting {
        val lastName = headers.getHeaderString("lastName")
        return Greeting("hello $firstName $lastName")
    }

    @GET @Path("noop") suspend fun noop() {}

    @POST
    @Path("body/{name}")
    suspend fun body(
        @PathParam(value = "name") name: String,
        greeting: Greeting,
        @Context uriInfo: UriInfo
    ) = Response.ok(greeting).build()

    @GET
    @Path("prop")
    fun testProp(): String? {
        return MY_PROPERTY.get()
    }
}

data class Greeting(val message: String)
