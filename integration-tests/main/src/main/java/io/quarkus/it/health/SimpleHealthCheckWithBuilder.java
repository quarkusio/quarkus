package io.quarkus.it.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public class SimpleHealthCheckWithBuilder implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("basic-with-builder").up().build();
    }

}
