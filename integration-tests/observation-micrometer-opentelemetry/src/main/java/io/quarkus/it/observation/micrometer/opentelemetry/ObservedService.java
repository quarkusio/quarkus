package io.quarkus.it.observation.micrometer.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.observation.annotation.Observed;

@ApplicationScoped
public class ObservedService {

    @Observed
    public String doWork() {
        return "observed-result";
    }

    @Observed
    public String customWork() {
        return "custom-observed-result";
    }
}
