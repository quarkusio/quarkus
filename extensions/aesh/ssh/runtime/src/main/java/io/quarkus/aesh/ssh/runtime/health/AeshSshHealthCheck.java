package io.quarkus.aesh.ssh.runtime.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.aesh.ssh.runtime.AeshSshConfig;
import io.quarkus.aesh.ssh.runtime.SshServerLifecycle;

@Readiness
@ApplicationScoped
public class AeshSshHealthCheck implements HealthCheck {

    @Inject
    SshServerLifecycle sshServer;

    @Inject
    AeshSshConfig config;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse
                .named("Aesh SSH server health check");

        if (!config.enabled()) {
            return builder.up().withData("server", "disabled").build();
        }

        if (sshServer.isRunning()) {
            return builder.up()
                    .withData("host", config.host())
                    .withData("port", config.port())
                    .withData("activeConnections", sshServer.getActiveSessionCount())
                    .build();
        } else {
            return builder.down()
                    .withData("reason", "SSH server is not running")
                    .build();
        }
    }
}
