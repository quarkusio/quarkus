package io.quarkus.it.observation.prometheus;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.observation.annotation.Observed;

@ApplicationScoped
public class ObservedService {

    @Observed
    public String doWork() {
        return "observed-result";
    }
}
