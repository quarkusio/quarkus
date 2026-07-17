package io.quarkus.it.observation.reactive;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
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
}
