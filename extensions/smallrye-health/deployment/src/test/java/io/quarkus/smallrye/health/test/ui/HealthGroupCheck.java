package io.quarkus.smallrye.health.test.ui;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.smallrye.health.api.HealthGroup;

@HealthGroup("test-group")
public class HealthGroupCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up(HealthGroupCheck.class.getName());
    }
}
