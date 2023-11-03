package io.quarkus.smallrye.health.runtime;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Singleton
@Readiness
public class ShutdownReadinessCheck implements HealthCheck {

    protected static final String GRACEFUL_SHUTDOWN = "Graceful Shutdown";
    private volatile boolean shuttingDown;

    public void shutdown() {
        shuttingDown = true;
    }

    @Override
    public HealthCheckResponse call() {
        if (shuttingDown) {
            return HealthCheckResponse.down(GRACEFUL_SHUTDOWN);
        }
        return HealthCheckResponse.up(GRACEFUL_SHUTDOWN);
    }
}
