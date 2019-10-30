package io.quarkus.artemis.core.runtime.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ServerLocatorHealthCheck implements HealthCheck {

    @Inject
    ServerLocator serverLocator;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Artemis Core health check");
        try (ClientSessionFactory factory = serverLocator.createSessionFactory()) {
            builder.up();
        } catch (Exception e) {
            builder.down();
        }
        return builder.build();
    }
}
