package io.quarkus.it.http3.rest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;

@Path("/")
public class HelloResource {

    @Inject
    HttpServerRequest request;

    @GET
    @Path("/hello")
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/version")
    public String version() {
        return request.version().name();
    }

    @POST
    @Path("/echo")
    public String echo(String body) {
        return body;
    }

    @GET
    @Path("/non-blocking")
    public Uni<String> nonBlocking() {
        return Uni.createFrom().item("non-blocking-response");
    }

    @GET
    @Path("/delayed")
    public Uni<String> delayed() {
        return Uni.createFrom().item("delayed-response")
                .onItem().delayIt().by(Duration.ofMillis(10));
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> stream() {
        return Multi.createFrom().items("event1", "event2", "event3");
    }

    @GET
    @Path("/completion-stage")
    public CompletionStage<String> completionStage() {
        return CompletableFuture.supplyAsync(() -> "completion-stage-response");
    }

    @GET
    @Path("/error")
    public Uni<String> error() {
        return Uni.createFrom().failure(new RuntimeException("test-error"));
    }
}
