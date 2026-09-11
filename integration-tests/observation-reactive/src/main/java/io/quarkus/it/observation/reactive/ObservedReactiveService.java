package io.quarkus.it.observation.reactive;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ObservedReactiveService {

    private static final AtomicInteger counter = new AtomicInteger();

    @Inject
    ObservationRegistry registry;

    @Observed
    public Uni<String> reactiveWork() {
        return Uni.createFrom().item("reactive-result")
                .invoke(() -> registry.getCurrentObservation().highCardinalityKeyValue("call.id",
                        "" + counter.getAndIncrement()))
                .onItem().delayIt().by(Duration.ofMillis(100));
    }

    @Observed
    public Multi<String> reactiveStream() {
        // The @Observed interceptor opens a single parent observation for the whole stream
        // (started/stopped once per subscription, on stream termination). Each emitted item then
        // opens its own child observation, parented to that stream observation via the current
        // scope, so every element of the stream produces its own child span.
        return Multi.createFrom().items("one", "two", "three")
                .onItem().transformToUniAndConcatenate(item -> {
                    Observation child = Observation.createNotStarted("stream.item.observation", registry);
                    child.lowCardinalityKeyValue("item", item);
                    child.start();
                    Observation.Scope scope = child.openScope();
                    return Uni.createFrom().item(item)
                            .onItem().delayIt().by(Duration.ofMillis(20))
                            .eventually(() -> {
                                scope.close();
                                child.stop();
                            });
                });
    }
}
