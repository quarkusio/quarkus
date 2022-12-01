package io.quarkus.it.resteasy.reactive.kotlin

import org.jboss.resteasy.reactive.RestHeader
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

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

    @POST
    @Path("body/{name}")
    suspend fun body(@PathParam(value = "name") name: String, greeting: Greeting, @Context uriInfo: UriInfo) = Response.ok(greeting).build()
}

data class Greeting(val message: String)
