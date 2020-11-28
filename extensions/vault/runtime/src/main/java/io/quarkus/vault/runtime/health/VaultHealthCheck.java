package io.quarkus.vault.runtime.health;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.sys.VaultHealth;

@Readiness
@Singleton
public class VaultHealthCheck implements HealthCheck {

    @Inject
    VaultSystemBackendEngine vaultSystemBackendEngine;

    @Override
    public HealthCheckResponse call() {

        final HealthCheckResponseBuilder builder = HealthCheckResponse.named("Vault connection health check");

        try {
            final VaultHealth vaultHealth = this.vaultSystemBackendEngine.health();

            if (vaultHealth.isInitializedUnsealedActive()) {
                builder.up();
            }

            if (vaultHealth.isUnsealedStandby()) {
                builder.down().withData("reason", "Unsealed and Standby");
            }

            if (vaultHealth.isRecoveryReplicationSecondary()) {
                builder.down().withData("reason", "Disaster recovery mode replication secondary and active");
            }

            if (vaultHealth.isPerformanceStandby()) {
                builder.down().withData("reason", "Performance standby");
            }

            if (vaultHealth.isNotInitialized()) {
                builder.down().withData("reason", "Not initialized");
            }

            if (vaultHealth.isSealed()) {
                builder.down().withData("reason", "Sealed");
            }

            return builder.build();

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }
}
