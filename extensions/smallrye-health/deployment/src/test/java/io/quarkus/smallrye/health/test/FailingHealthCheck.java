package io.quarkus.smallrye.health.test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.health.api.HealthGroup;

@Dependent
@Liveness
@Readiness
@HealthGroup("group1")
@HealthGroup("group2")
public class FailingHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return new HealthCheckResponse() {
            @Override
            public String getName() {
                return "failing";
            }

            @Override
            public State getState() {
                return State.DOWN;
            }

            @Override
            public Optional<Map<String, Object>> getData() {
                return Optional.of(Collections.singletonMap("status", "all broken"));
            }
        };
    }
}
