package io.quarkus.vault.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.config.CredentialsProviderConfig;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

@ApplicationScoped
@Named("vault-credentials-provider")
public class VaultCredentialsProvider implements CredentialsProvider {

    @Inject
    private VaultKVSecretEngine vaultKVSecretEngine;
    @Inject
    private VaultDbManager vaultDbManager;
    @Inject
    private VaultConfigHolder vaultConfigHolder;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {

        CredentialsProviderConfig config = getConfig().credentialsProvider.get(credentialsProviderName);

        if (config == null) {
            throw new VaultException("unknown credentials provider with name " + credentialsProviderName);
        }

        if (config.databaseCredentialsRole.isPresent()) {
            return vaultDbManager.getDynamicDbCredentials(config.databaseCredentialsRole.get());
        }

        if (config.kvPath.isPresent()) {
            String password = vaultKVSecretEngine.readSecret(config.kvPath.get()).get(config.kvKey);
            Map<String, String> result = new HashMap<>();
            result.put(PASSWORD_PROPERTY_NAME, password);
            return result;
        }

        throw new VaultException(
                "one of database-credentials-role or kv-path is required on credentials provider " + credentialsProviderName);
    }

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }
}
