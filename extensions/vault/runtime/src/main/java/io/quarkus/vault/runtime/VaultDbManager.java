package io.quarkus.vault.runtime;

import static io.quarkus.vault.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.vault.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultDbManager {

    private static final Logger log = Logger.getLogger(VaultDbManager.class.getName());

    ConcurrentHashMap<String, VaultDynamicDatabaseCredentials> credentialsCache = new ConcurrentHashMap<>();
    private VaultAuthManager vaultAuthManager;
    private VaultClient vaultClient;
    private VaultRuntimeConfig serverConfig;

    public VaultDbManager(VaultAuthManager vaultAuthManager, VaultClient vaultClient, VaultRuntimeConfig serverConfig) {
        this.vaultAuthManager = vaultAuthManager;
        this.vaultClient = vaultClient;
        this.serverConfig = serverConfig;
    }

    public Properties getDynamicDbCredentials(String databaseCredentialsRole) {
        String clientToken = vaultAuthManager.getClientToken();
        VaultDynamicDatabaseCredentials currentCredentials = credentialsCache.get(databaseCredentialsRole);
        VaultDynamicDatabaseCredentials credentials = getCredentials(currentCredentials, clientToken, databaseCredentialsRole);
        credentialsCache.put(databaseCredentialsRole, credentials);
        Properties properties = new Properties();
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
        if (credentials != null && credentials.shouldExtend(serverConfig.renewGracePeriod)) {
            credentials = extend(credentials, clientToken, databaseCredentialsRole);
        }

        // create lease if necessary
        if (credentials == null || credentials.isExpired() || credentials.expiresSoon(serverConfig.renewGracePeriod)) {
            credentials = create(clientToken, databaseCredentialsRole);
        }

        return credentials;
    }

    private VaultDynamicDatabaseCredentials validate(VaultDynamicDatabaseCredentials credentials, String clientToken) {
        try {
            vaultClient.lookupLease(clientToken, credentials.leaseId);
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
        VaultRenewLease vaultRenewLease = vaultClient.renewLease(clientToken, currentCredentials.leaseId);
        LeaseBase lease = new LeaseBase(vaultRenewLease.leaseId, vaultRenewLease.renewable, vaultRenewLease.leaseDurationSecs);
        VaultDynamicDatabaseCredentials credentials = new VaultDynamicDatabaseCredentials(lease, currentCredentials.username,
                currentCredentials.password);
        sanityCheck(credentials, databaseCredentialsRole);
        log.debug("extended " + databaseCredentialsRole + " credentials with: "
                + credentials.getConfidentialInfo(serverConfig.logConfidentialityLevel));
        return credentials;
    }

    private VaultDynamicDatabaseCredentials create(String clientToken, String databaseCredentialsRole) {
        VaultDatabaseCredentials vaultDatabaseCredentials = vaultClient.generateDatabaseCredentials(clientToken,
                databaseCredentialsRole);
        LeaseBase lease = new LeaseBase(vaultDatabaseCredentials.leaseId, vaultDatabaseCredentials.renewable,
                vaultDatabaseCredentials.leaseDurationSecs);
        VaultDynamicDatabaseCredentials credentials = new VaultDynamicDatabaseCredentials(lease,
                vaultDatabaseCredentials.data.username,
                vaultDatabaseCredentials.data.password);
        log.debug("generated " + databaseCredentialsRole + " credentials: "
                + credentials.getConfidentialInfo(serverConfig.logConfidentialityLevel));
        sanityCheck(credentials, databaseCredentialsRole);
        return credentials;
    }

    private void sanityCheck(VaultDynamicDatabaseCredentials credentials, String databaseCredentialsRole) {
        credentials.leaseDurationSanityCheck(databaseCredentialsRole, serverConfig.renewGracePeriod);
    }

}
