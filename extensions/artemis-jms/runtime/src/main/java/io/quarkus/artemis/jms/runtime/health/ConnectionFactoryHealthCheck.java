package io.quarkus.artemis.jms.runtime.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ConnectionFactoryHealthCheck implements HealthCheck {

    @Inject
    ConnectionFactory connectionFactory;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Artemis JMS health check");
        try (Connection connection = connectionFactory.createConnection()) {
            builder.up();
        } catch (Exception e) {
            builder.down();
        }
        return builder.build();
    }
}
