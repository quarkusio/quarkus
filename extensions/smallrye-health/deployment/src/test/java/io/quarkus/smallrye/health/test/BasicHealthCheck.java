package io.quarkus.smallrye.health.test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Dependent
@Liveness
public class BasicHealthCheck implements HealthCheck {

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
                return Optional.of(Collections.singletonMap("foo", "bar"));
            }
        };
    }
}
