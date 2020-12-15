package io.quarkus.it.health;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public class SimpleHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return new HealthCheckResponse() {
            @Override
            public String getName() {
                return "basic";
            }

            @Override
            public Status getStatus() {
                return Status.UP;
            }

            @Override
            public Optional<Map<String, Object>> getData() {
                return Optional.empty();
            }
        };
    }
}
