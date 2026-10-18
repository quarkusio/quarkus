package io.quarkus.it.opentelemetry.reactive;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceException;

@Path("/reactive")
public class ReactiveResource {
    public static final int MILLISECONDS_DELAY = 100;
    @Inject
    Tracer tracer;
    @Inject
    @RestClient
    ReactiveRestClient client;

    private ScheduledExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newScheduledThreadPool(2);
    }

    @GET
    public Uni<String> helloGet(@QueryParam("name") String name) {
        Span span = tracer.spanBuilder("helloGet").startSpan();
        return Uni.createFrom().item("Hello " + name).onItem().delayIt().by(Duration.ofMillis(MILLISECONDS_DELAY))
                .eventually((Runnable) span::end);
    }

    @GET
    @Path("/hello-get-uni-delay")
    @WithSpan("helloGetUniDelay")
    public Uni<String> helloGetUniDelay() {
        return Uni.createFrom().item("helloGetUniDelay").onItem().delayIt().by(Duration.ofMillis(MILLISECONDS_DELAY));
    }

    @GET
    @Path("/hello-get-uni-executor")
    @WithSpan("helloGetUniExecutor")
    public Uni<String> helloGetUniExecutor() {
        return Uni.createFrom().item("helloGetUniExecutor")
                .onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(MILLISECONDS_DELAY));
    }

    @GET
    @Path("/multiple-chain")
    public Uni<String> helloMultipleUsingChain() {
        return client.helloGet("Naruto")
                .chain(s1 -> client.helloGet("Goku").map(s2 -> s1 + " and " + s2));
    }

    @GET
    @Path("/multiple-combine")
    public Uni<String> helloMultipleUsingCombine() {
        return Uni.combine().all().unis(
                client.helloGet("Naruto"),
                client.helloGet("Goku"))
                .combinedWith((s, s2) -> s + " and " + s2);
    }

    @GET
    @Path("/multiple-combine-different-paths")
    public Uni<String> helloMultipleUsingCombineDifferentPaths() {
        return Uni.combine().all().unis(
                client.helloGetUniDelay(),
                client.helloGet("Naruto"),
                client.helloGet("Goku"),
                client.helloGetUniExecutor())
                .with((s, s2, s3, s4) -> s + " and " + s2 + " and " + s3 + " and " + s4);
    }

    @POST
    public Uni<String> helloPost(String body) {
        Span span = tracer.spanBuilder("helloPost").startSpan();
        return Uni.createFrom().item("Hello " + body).onItem().delayIt().by(Duration.ofMillis(MILLISECONDS_DELAY))
                .eventually((Runnable) span::end);
    }

    @Path("blockingException")
    @GET
    public String blockingException() {
        throw new NoStackTraceException("dummy");
    }

    @Path("reactiveException")
    @GET
    public Uni<String> reactiveException() {
        return Uni.createFrom().item(() -> {
            throw new NoStackTraceException("dummy2");
        });
    }
}
