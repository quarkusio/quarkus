package org.acme

import io.quarkus.grpc.GrpcClient
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.acme.module1.SomeClass1
import org.acme.proto.Greeter

@Path("/version")
class GreetingResource {

    @GrpcClient
    lateinit var hello: Greeter

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getVersion(): String {
        return SomeClass1().getVersion()
    }
}

