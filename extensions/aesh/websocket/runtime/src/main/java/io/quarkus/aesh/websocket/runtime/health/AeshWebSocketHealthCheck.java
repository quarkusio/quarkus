package io.quarkus.aesh.websocket.runtime.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.aesh.websocket.runtime.AeshWebSocketEndpoint;
import io.quarkus.aesh.websocket.runtime.AeshWebSocketPath;

@Readiness
@ApplicationScoped
public class AeshWebSocketHealthCheck implements HealthCheck {

    @Inject
    AeshWebSocketEndpoint endpoint;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("Aesh WebSocket terminal health check")
                .up()
                .withData("path", AeshWebSocketPath.getPath())
                .withData("activeConnections", endpoint.getActiveConnectionCount())
                .build();
    }
}
