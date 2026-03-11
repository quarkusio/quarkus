package io.quarkus.narayana.jta.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item indicating whether transaction recovery is enabled.
 * <p>
 * This serves as the single source of truth for the recovery-enabled decision,
 * which is derived from {@code quarkus.transaction-manager.enable-recovery}
 * (defaulting to {@code true} when XA datasources are present).
 */
public final class TransactionRecoveryEnabledBuildItem extends SimpleBuildItem {

    private final boolean enabled;

    public TransactionRecoveryEnabledBuildItem(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
