package io.quarkus.vault;

import java.util.Map;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

/**
 * This service provides access to the kv secret engine, taking care of authentication,
 * and token extension or renewal, according to ttl and max-ttl.
 *
 * @see VaultRuntimeConfig
 */
public interface VaultKVSecretEngine {

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Map<String, String> readSecret(String path);

}
