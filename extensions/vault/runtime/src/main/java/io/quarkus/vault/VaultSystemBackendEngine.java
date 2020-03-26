package io.quarkus.vault;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultSealStatus;

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
     * Returns Vault health status code only by using HTTP HEAD requests.
     * It is faster than calling {@link #healthStatus() healthStatus()} method which uses HTTP GET to return a complete
     * VaultHealthStatus state.
     *
     * @return Vault Health Status.
     */
    VaultHealth health();

    /**
     * Check and return the health status of Vault.
     * Returns a complete VaultHealthStatus state.
     * This method uses HTTP GET to return a complete state.
     *
     * @return Complete Vault Health Status.
     */
    VaultHealthStatus healthStatus();

    /**
     * Check the seal status of a Vault.
     *
     * @return Vault Seal Status.
     */
    VaultSealStatus sealStatus();
}
