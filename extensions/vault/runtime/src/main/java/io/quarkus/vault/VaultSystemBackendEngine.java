package io.quarkus.vault;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.sys.health.VaultHealth;
import io.quarkus.vault.runtime.sys.health.VaultHealthStatus;
import io.quarkus.vault.runtime.sys.seal.VaultInit;
import io.quarkus.vault.runtime.sys.seal.VaultSealStatus;

/**
 * This service provides access to the system backend.
 *
 * @see VaultRuntimeConfig
 */
public interface VaultSystemBackendEngine {

    /**
     * Initializes a new Vault.
     * 
     * @param secretShares specifies the number of shares to split the master key into.
     * @param secretThreshold specifies the number of shares required to reconstruct the master key.
     * @return Vault Init.
     */
    VaultInit init(int secretShares, int secretThreshold);

    /**
     * Check the health status of Vault.
     * 
     * @return Vault Health Status.
     */
    VaultHealth health();

    /**
     * Check and return the health status of Vault.
     *
     * @return Vault Health Status.
     */
    VaultHealthStatus healthStatus();

    /**
     * Check the seal status of a Vault.
     * 
     * @return Vault Seal Status.
     */
    VaultSealStatus sealStatus();
}
