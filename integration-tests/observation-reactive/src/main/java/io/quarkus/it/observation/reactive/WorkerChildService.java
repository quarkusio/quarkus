package io.quarkus.it.observation.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.observation.annotation.Observed;

/**
 * A synchronous {@code @Observed} bean used to verify that an Observation scope opened on the
 * request thread is propagated to another thread (worker or virtual) so the child observation is
 * parented to it.
 */
@ApplicationScoped
public class WorkerChildService {

    @Observed
    public String doChildWork() {
        return "child-done";
    }
}
