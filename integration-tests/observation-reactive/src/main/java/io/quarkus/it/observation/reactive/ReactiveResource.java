package io.quarkus.it.observation.reactive;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/reactive")
public class ReactiveResource {

    @Inject
    ObservationRegistry registry;

    @Inject
    ObservedReactiveService service;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    WorkerChildService childService;

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
    @Path("/observed-multi")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> observedMulti() {
        // The @Observed method returns a Multi. The interceptor must produce a single
        // observation span for the whole stream (one subscription), parented to the request.
        return service.reactiveStream();
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
    @Path("/worker")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> worker() {
        // Open the parent observation on the request thread (a Vert.x duplicated context).
        Observation observation = Observation.createNotStarted("worker.parent.observation", registry);
        observation.start();
        Observation.Scope scope = observation.openScope();
        // Offload the child @Observed work to a worker thread. The observation scope must be
        // propagated across the thread boundary (via the duplicated context and the MP context
        // propagation provider) so the child observation is parented to the parent.
        return managedExecutor.supplyAsync(() -> childService.doChildWork())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        observation.error(throwable);
                    }
                    scope.close();
                    observation.stop();
                });
    }

    @GET
    @Path("/virtual")
    @Produces(MediaType.TEXT_PLAIN)
    @RunOnVirtualThread
    public String virtual() {
        // Runs on a virtual thread (or the blocking worker pool on JDK < 21) with the request's
        // duplicated context active. The nested @Observed child must be parented to the parent.
        Observation observation = Observation.createNotStarted("virtual.parent.observation", registry);
        observation.start();
        try (Observation.Scope scope = observation.openScope()) {
            return childService.doChildWork();
        } finally {
            observation.stop();
        }
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
