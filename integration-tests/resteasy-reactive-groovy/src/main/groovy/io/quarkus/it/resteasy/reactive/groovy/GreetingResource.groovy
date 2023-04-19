package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.jboss.resteasy.reactive.RestHeader

@RegisterForReflection(targets = [HttpHeaders.class])
@Path("/greeting")
class GreetingResource {

    @Inject
    HttpHeaders headers

    @GET
    Greeting testSuspend(@RestHeader("firstName") String firstName) {
        var lastName = headers.getHeaderString("lastName")
        new Greeting("hello $firstName $lastName")
    }

    @GET @Path("noop") def noop() {}

    @POST
    @Path("body/{name}")
    def body(
        @PathParam(value = "name") String name,
        Greeting greeting,
        @Context UriInfo uriInfo
    ) {
        Response.ok(greeting).build()
    }
}

@RegisterForReflection
class Greeting {
    String message
    Greeting(String message) {
        this.message = message
    }
}
