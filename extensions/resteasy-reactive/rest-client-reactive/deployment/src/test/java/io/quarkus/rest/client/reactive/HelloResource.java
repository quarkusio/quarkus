package io.quarkus.rest.client.reactive;

import java.time.Duration;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;

import io.smallrye.mutiny.Uni;

@Path("/hello")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class HelloResource {
    @POST
    public String echo(String name, @Context Request request) {
        return "hello, " + name;
    }

    @RestClient
    HelloClient2 client2;

    @GET
    public Uni<String> something() {
        Thread thread = Thread.currentThread();
        return client2.delay()
                .map(foo -> {
                    Assertions.assertSame(thread, Thread.currentThread());
                    return foo;
                });
    }

    @Path("delay")
    @GET
    public Uni<String> delay() {
        return Uni.createFrom().item("Hello")
                .onItem().delayIt().by(Duration.ofMillis(500));
    }
}
