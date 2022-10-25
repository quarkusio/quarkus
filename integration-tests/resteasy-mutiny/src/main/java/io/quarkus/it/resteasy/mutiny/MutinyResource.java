package io.quarkus.it.resteasy.mutiny;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/mutiny")
public class MutinyResource {

    @Inject
    SomeService service;

    @GET
    @Path("/hello")
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
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Pet> sse() {
        return service.getMorePets();
    }

    @Inject
    @RestClient
    MyRestService client;

    @GET
    @Path("/client")
    public Uni<String> callHello() {
        return client.hello();
    }

    @GET
    @Path("/client/pet")
    public Uni<Pet> callPet() {
        return client.pet();
    }

}
