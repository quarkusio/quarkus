package io.quarkus.vault.runtime;

import static io.quarkus.credentials.CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDynamicCredentialsSecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

@Singleton
public class VaultDynamicCredentialsManager {

    private static final Logger log = Logger.getLogger(VaultDynamicCredentialsManager.class.getName());

    private ConcurrentHashMap<String, VaultDynamicCredentials> credentialsCache = new ConcurrentHashMap<>();
    private VaultAuthManager vaultAuthManager;
    private VaultConfigHolder vaultConfigHolder;
    private VaultInternalSystemBackend vaultInternalSystemBackend;
    private VaultInternalDynamicCredentialsSecretEngine vaultInternalDynamicCredentialsSecretEngine;

    public VaultDynamicCredentialsManager(VaultConfigHolder vaultConfigHolder, VaultAuthManager vaultAuthManager,
            VaultInternalSystemBackend vaultInternalSystemBackend,
            VaultInternalDynamicCredentialsSecretEngine vaultInternalDynamicCredentialsSecretEngine) {
        this.vaultConfigHolder = vaultConfigHolder;
        this.vaultAuthManager = vaultAuthManager;
        this.vaultInternalSystemBackend = vaultInternalSystemBackend;
        this.vaultInternalDynamicCredentialsSecretEngine = vaultInternalDynamicCredentialsSecretEngine;
    }

    private String getCredentialsPath(String mount, String requestPath) {
        return mount + "/" + requestPath;
    }

    private String getCredentialsCacheKey(String mount, String requestPath, String role) {
        return getCredentialsPath(mount, requestPath) + "@" + role;
    }

    VaultDynamicCredentials getCachedCredentials(String mount, String requestPath, String role) {
        return credentialsCache.get(getCredentialsCacheKey(mount, requestPath, role));
    }

    void putCachedCredentials(String mount, String requestPath, String role, VaultDynamicCredentials credentials) {
        credentialsCache.put(getCredentialsCacheKey(mount, requestPath, role), credentials);
    }

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    public Map<String, String> getDynamicCredentials(String mount, String requestPath, String role) {
        String clientToken = vaultAuthManager.getClientToken();
        VaultDynamicCredentials currentCredentials = getCachedCredentials(mount, requestPath, role);
        VaultDynamicCredentials credentials = getCredentials(currentCredentials, clientToken, mount, requestPath, role);
        putCachedCredentials(mount, requestPath, role, credentials);
        Map<String, String> properties = new HashMap<>();
        properties.put(USER_PROPERTY_NAME, credentials.username);
        properties.put(PASSWORD_PROPERTY_NAME, credentials.password);
        properties.put(EXPIRATION_TIMESTAMP_PROPERTY_NAME, credentials.getExpireInstant().toString());
        return properties;
    }

    public VaultDynamicCredentials getCredentials(VaultDynamicCredentials currentCredentials,
            String clientToken, String mount, String requestPath, String role) {

        VaultDynamicCredentials credentials = currentCredentials;

        // check lease is still valid
        if (credentials != null) {
            credentials = validate(credentials, clientToken);
        }

        // extend lease if necessary
        if (credentials != null && credentials.shouldExtend(getConfig().renewGracePeriod)) {
            credentials = extend(credentials, clientToken, mount, requestPath, role);
        }

        // create lease if necessary
        if (credentials == null || credentials.isExpired()
                || credentials.expiresSoon(getConfig().renewGracePeriod)) {
            credentials = create(clientToken, mount, requestPath, role);
        }

        return credentials;
    }

    private VaultDynamicCredentials validate(VaultDynamicCredentials credentials, String clientToken) {
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

    private VaultDynamicCredentials extend(VaultDynamicCredentials currentCredentials, String clientToken,
            String mount, String requestPath, String role) {
        VaultRenewLease vaultRenewLease = vaultInternalSystemBackend.renewLease(clientToken, currentCredentials.leaseId);
        LeaseBase lease = new LeaseBase(vaultRenewLease.leaseId, vaultRenewLease.renewable, vaultRenewLease.leaseDurationSecs);
        VaultDynamicCredentials credentials = new VaultDynamicCredentials(lease, currentCredentials.username,
                currentCredentials.password);
        sanityCheck(credentials, mount, requestPath, role);
        log.debug("extended " + role + "(" + getCredentialsPath(mount, requestPath) + ") credentials:"
                + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel));
        return credentials;
    }

    private VaultDynamicCredentials create(String clientToken, String mount, String requestPath, String role) {
        io.quarkus.vault.runtime.client.dto.dynamic.VaultDynamicCredentials vaultDynamicCredentials = vaultInternalDynamicCredentialsSecretEngine
                .generateCredentials(clientToken, mount, requestPath, role);
        LeaseBase lease = new LeaseBase(vaultDynamicCredentials.leaseId, vaultDynamicCredentials.renewable,
                vaultDynamicCredentials.leaseDurationSecs);
        VaultDynamicCredentials credentials = new VaultDynamicCredentials(lease,
                vaultDynamicCredentials.data.username,
                vaultDynamicCredentials.data.password);
        log.debug("generated " + role + "(" + getCredentialsPath(mount, requestPath) + ") credentials:"
                + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel));
        sanityCheck(credentials, mount, requestPath, role);
        return credentials;
    }

    private void sanityCheck(VaultDynamicCredentials credentials, String mount, String requestPath, String role) {
        credentials.leaseDurationSanityCheck(role + " (" + getCredentialsPath(mount, requestPath) + ")",
                getConfig().renewGracePeriod);
    }
}
