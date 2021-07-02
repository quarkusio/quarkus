package io.quarkus.it.resteasy.reactive;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestSseElementType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/reactive")
@Produces(MediaType.APPLICATION_JSON)
public class ReactiveMutinyResource {

    @Inject
    io.quarkus.it.resteasy.reactive.SomeService service;

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello() {
        return service.greeting();
    }

    @GET
    @Path("/fail")
    public Uni<String> fail() {
        return Uni.createFrom().failure(new IOException("boom"));
    }

    @GET
    @Path("/response")
    public Uni<Response> response() {
        return service.greeting()
                .onItem().transform(v -> Response.accepted().type("text/plain").entity(v).build());
    }

    @GET
    @Path("/hello/stream")
    public Multi<String> helloAsMulti() {
        return service.greetingAsMulti();
    }

    @GET
    @Path("/pet")
    public Uni<Pet> pet() {
        return service.getPet();
    }

    @GET
    @Path("/pet/stream")
    public Multi<Pet> pets() {
        return service.getPets();
    }

    @GET
    @Path("/pets")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    public Multi<Pet> sse() {
        return service.getMorePets();
    }

    @Inject
    @RestClient
    MyRestService client;

    @GET
    @Path("/client")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> callHello() {
        return client.hello();
    }

    @GET
    @Path("/client/pet")
    public Uni<Pet> callPet() {
        return client.pet();
    }

}
