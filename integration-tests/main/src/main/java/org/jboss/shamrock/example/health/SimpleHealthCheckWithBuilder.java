package org.jboss.shamrock.example.health;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@Health
public class SimpleHealthCheckWithBuilder implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("basic-with-builder").up().build();
    }

}
