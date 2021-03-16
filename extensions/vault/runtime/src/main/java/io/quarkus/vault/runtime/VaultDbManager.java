package io.quarkus.vault.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDatabaseSecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

@Singleton
public class VaultDbManager {

    private static final Logger log = Logger.getLogger(VaultDbManager.class.getName());

    ConcurrentHashMap<String, VaultDynamicDatabaseCredentials> credentialsCache = new ConcurrentHashMap<>();
    private VaultAuthManager vaultAuthManager;
    private VaultConfigHolder vaultConfigHolder;
    private VaultInternalSystemBackend vaultInternalSystemBackend;
    private VaultInternalDatabaseSecretEngine vaultInternalDatabaseSecretEngine;

    public VaultDbManager(VaultConfigHolder vaultConfigHolder, VaultAuthManager vaultAuthManager,
            VaultInternalSystemBackend vaultInternalSystemBackend,
            VaultInternalDatabaseSecretEngine vaultInternalDatabaseSecretEngine) {
        this.vaultConfigHolder = vaultConfigHolder;
        this.vaultAuthManager = vaultAuthManager;
        this.vaultInternalSystemBackend = vaultInternalSystemBackend;
        this.vaultInternalDatabaseSecretEngine = vaultInternalDatabaseSecretEngine;
    }

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    public Map<String, String> getDynamicDbCredentials(String databaseCredentialsRole) {
        String clientToken = vaultAuthManager.getClientToken();
        VaultDynamicDatabaseCredentials currentCredentials = credentialsCache.get(databaseCredentialsRole);
        VaultDynamicDatabaseCredentials credentials = getCredentials(currentCredentials, clientToken, databaseCredentialsRole);
        credentialsCache.put(databaseCredentialsRole, credentials);
        Map<String, String> properties = new HashMap<>();
        properties.put(USER_PROPERTY_NAME, credentials.username);
        properties.put(PASSWORD_PROPERTY_NAME, credentials.password);
        return properties;
    }

    public VaultDynamicDatabaseCredentials getCredentials(VaultDynamicDatabaseCredentials currentCredentials,
            String clientToken,
            String databaseCredentialsRole) {

        VaultDynamicDatabaseCredentials credentials = currentCredentials;

        // check lease is still valid
        if (credentials != null) {
            credentials = validate(credentials, clientToken);
        }

        // extend lease if necessary
        if (credentials != null && credentials.shouldExtend(getConfig().renewGracePeriod)) {
            credentials = extend(credentials, clientToken, databaseCredentialsRole);
        }

        // create lease if necessary
        if (credentials == null || credentials.isExpired()
                || credentials.expiresSoon(getConfig().renewGracePeriod)) {
            credentials = create(clientToken, databaseCredentialsRole);
        }

        return credentials;
    }

    private VaultDynamicDatabaseCredentials validate(VaultDynamicDatabaseCredentials credentials, String clientToken) {
        try {
            vaultInternalSystemBackend.lookupLease(clientToken, credentials.leaseId);
            return credentials;
        } catch (VaultClientException e) {
            if (e.getStatus() == 400) { // bad request
                log.debug("lease " + credentials.leaseId + " has become invalid");
                return null;
            } else {
                throw e;
            }
        }
    }

    private VaultDynamicDatabaseCredentials extend(VaultDynamicDatabaseCredentials currentCredentials, String clientToken,
            String databaseCredentialsRole) {
        VaultRenewLease vaultRenewLease = vaultInternalSystemBackend.renewLease(clientToken, currentCredentials.leaseId);
        LeaseBase lease = new LeaseBase(vaultRenewLease.leaseId, vaultRenewLease.renewable, vaultRenewLease.leaseDurationSecs);
        VaultDynamicDatabaseCredentials credentials = new VaultDynamicDatabaseCredentials(lease, currentCredentials.username,
                currentCredentials.password);
        sanityCheck(credentials, databaseCredentialsRole);
        log.debug("extended " + databaseCredentialsRole + " credentials with: "
                + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel));
        return credentials;
    }

    private VaultDynamicDatabaseCredentials create(String clientToken, String databaseCredentialsRole) {
        VaultDatabaseCredentials vaultDatabaseCredentials = vaultInternalDatabaseSecretEngine.generateCredentials(clientToken,
                databaseCredentialsRole);
        LeaseBase lease = new LeaseBase(vaultDatabaseCredentials.leaseId, vaultDatabaseCredentials.renewable,
                vaultDatabaseCredentials.leaseDurationSecs);
        VaultDynamicDatabaseCredentials credentials = new VaultDynamicDatabaseCredentials(lease,
                vaultDatabaseCredentials.data.username,
                vaultDatabaseCredentials.data.password);
        log.debug("generated " + databaseCredentialsRole + " credentials: "
                + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel));
        sanityCheck(credentials, databaseCredentialsRole);
        return credentials;
    }

    private void sanityCheck(VaultDynamicDatabaseCredentials credentials, String databaseCredentialsRole) {
        credentials.leaseDurationSanityCheck(databaseCredentialsRole, getConfig().renewGracePeriod);
    }
}
