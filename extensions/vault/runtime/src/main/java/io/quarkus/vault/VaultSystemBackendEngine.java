package io.quarkus.vault;

import java.util.List;

import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultTuneInfo;

/**
 * This service provides access to the system backend.
 *
 * @see VaultBootstrapConfig
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

    /**
     * Get the rules for the named policy.
     * 
     * @param name of the policy
     * @return rules of named policy
     */
    String getPolicyRules(String name);

    /**
     * Create or update a policy.
     *
     * @param name policy name
     * @param rules policy content
     */
    void createUpdatePolicy(String name, String rules);

    /**
     * Delete a policy by its name.
     *
     * @param name policy name
     */
    void deletePolicy(String name);

    /**
     * List existing policies.
     * 
     * @return a list of all policy names
     */
    List<String> getPolicies();

    /**
     * Get the tune info for a secret engine at a specific mount.
     *
     * @param mount Name of the mount
     * @return current tune info
     */
    VaultTuneInfo getTuneInfo(String mount);

    /**
     * Update the tune info for a secret engine at a specific mount.
     *
     * @param mount Name of the mount
     * @param tuneInfo Tune info with fields to update
     */
    void updateTuneInfo(String mount, VaultTuneInfo tuneInfo);

    /**
     * Check if an engine is mounted at a specific mount.
     *
     * @param mount Name of the mount
     * @return True if an engine is mounted, false otherwise
     */
    boolean isEngineMounted(String mount);

    /**
     * Enables a secret engine at a specific mount.
     *
     * @param engine Type of engine to mount.
     * @param mount Engine mount path.
     * @param description Human friendly description of mount point.
     * @param options Engine options.
     */
    void enable(VaultSecretEngine engine, String mount, String description, EnableEngineOptions options);

    /**
     * Enables a secret engine at a specific mount.
     *
     * @param engineType Type of engine to mount.
     * @param mount Engine mount path.
     * @param description Human friendly description of mount point.
     * @param options Engine options.
     */
    void enable(String engineType, String mount, String description, EnableEngineOptions options);

    /**
     * Disables the engine at a specific mount.
     *
     * @param mount Engine mount path.
     */
    void disable(String mount);

}
