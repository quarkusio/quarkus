package io.quarkus.it.resteasy.reactive.kotlin

import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/greeting")
class GreetingResource {

    @GET
    suspend fun testSuspend() = Greeting("hello")
}

data class Greeting(val message:String)
