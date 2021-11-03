package io.quarkus.it.resteasy.reactive.kotlin

import org.jboss.resteasy.reactive.RestHeader
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/greeting")
class GreetingResource {

    @GET
    suspend fun testSuspend(@RestHeader("firstName") firstName: String, @RestHeader("lastName") lastName: String) = Greeting("hello $firstName $lastName")
}

data class Greeting(val message:String)
