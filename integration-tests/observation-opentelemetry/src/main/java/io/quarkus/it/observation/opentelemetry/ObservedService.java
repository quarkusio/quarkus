package io.quarkus.it.observation.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.observation.annotation.Observed;

@ApplicationScoped
public class ObservedService {

    @Observed(name = "service.work", lowCardinalityKeyValues = { "service.type", "observed" })
    public String doWork() {
        return "observed-result";
    }
}
