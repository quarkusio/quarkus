package io.quarkus.vault;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

/**
 * This service provides access to the kv secret engine, taking care of authentication,
 * and token extension or renewal, according to ttl and max-ttl.
 *
 * @see VaultBootstrapConfig
 */
public interface VaultKVSecretEngine {

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Map<String, String> readSecret(String path);

    /**
     * Writes the secret at the given path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    void writeSecret(String path, Map<String, String> secret);

    /**
     * Deletes the secret at the given path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param path to delete
     */
    void deleteSecret(String path);

    /**
     * List all paths under the specified path.
     *
     * @param path to list
     * @return list of subpaths
     */
    List<String> listSecrets(String path);

}
