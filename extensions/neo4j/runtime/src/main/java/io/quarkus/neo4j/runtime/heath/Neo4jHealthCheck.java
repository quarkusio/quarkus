package io.quarkus.neo4j.runtime.heath;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.neo4j.driver.Driver;

@Readiness
@ApplicationScoped
public class Neo4jHealthCheck implements HealthCheck {
    @Inject
    Driver driver;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Neo4j connection health check").up();
        try {
            driver.verifyConnectivity();
            return builder.build();
        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }
}
