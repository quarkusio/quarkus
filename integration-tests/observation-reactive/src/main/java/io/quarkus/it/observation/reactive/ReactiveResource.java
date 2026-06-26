package io.quarkus.it.observation.reactive;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.smallrye.mutiny.Uni;

@Path("/reactive")
public class ReactiveResource {

    @Inject
    ObservationRegistry registry;

    @Inject
    ObservedReactiveService service;

    @RestClient
    ReactiveRestClient client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> helloGet(@QueryParam("name") String name) {
        Observation observation = Observation.createNotStarted("reactive.hello.observation", registry);
        observation.lowCardinalityKeyValue("name", name != null ? name : "world");
        observation.start();
        Observation.Scope scope = observation.openScope();
        return Uni.createFrom().item("Hello " + name)
                .onItem().delayIt().by(Duration.ofMillis(100))
                .onFailure().invoke(observation::error)
                .eventually(() -> {
                    scope.close();
                    observation.stop();
                });
    }

    @GET
    @Path("/observed-uni")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> observedUni() {
        return service.reactiveWork();
    }

    @GET
    @Path("/multiple-chain")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> multipleChain() {
        Observation observation = Observation.createNotStarted("chain.operation.observation", registry);
        observation.start();
        Observation.Scope scope = observation.openScope();
        return client.helloGet("Naruto")
                .chain(s1 -> client.helloGet("Goku").map(s2 -> s1 + " and " + s2))
                .onFailure().invoke(observation::error)
                .eventually(() -> {
                    scope.close();
                    observation.stop();
                });
    }

    @GET
    @Path("/multiple-combine")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> multipleCombine() {
        Observation observation = Observation.createNotStarted("combine.operation.observation", registry);
        observation.start();
        Observation.Scope scope = observation.openScope();
        return Uni.combine().all().unis(
                client.helloGet("Naruto"),
                client.helloGet("Goku"))
                .combinedWith((s1, s2) -> s1 + " and " + s2)
                .onFailure().invoke(observation::error)
                .eventually(() -> {
                    scope.close();
                    observation.stop();
                });
    }

    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> reactiveError() {
        Observation observation = Observation.createNotStarted("error.operation.observation", registry);
        observation.start();
        Observation.Scope scope = observation.openScope();
        return Uni.createFrom().<String> failure(new RuntimeException("reactive error"))
                .onFailure().invoke(observation::error)
                .onTermination().invoke(() -> {
                    scope.close();
                    observation.stop();
                })
                .onFailure().recoverWithItem("error-handled");
    }
}
