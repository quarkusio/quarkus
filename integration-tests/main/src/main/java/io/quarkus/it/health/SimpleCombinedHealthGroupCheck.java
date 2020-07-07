package io.quarkus.it.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.smallrye.health.api.HealthGroup;

@HealthGroup("group1")
@HealthGroup("group2")
public class SimpleCombinedHealthGroupCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("combined");
    }
}
