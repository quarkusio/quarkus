package io.quarkus.vault.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.VertxVaultClient;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalAppRoleAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalKubernetesAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalTokenAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalUserpassAuthMethod;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentialsData;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDatabaseSecretEngine;
import io.quarkus.vault.runtime.config.VaultAppRoleAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.quarkus.vault.runtime.config.VaultKubernetesAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;
import io.quarkus.vault.runtime.config.VaultUserpassAuthenticationConfig;

public class VaultDbManagerTest {

    VaultBootstrapConfig config = createConfig();
    TlsConfig tlsConfig = new TlsConfig();
    VaultDatabaseCredentials credentials = new VaultDatabaseCredentials();
    VaultLeasesLookup vaultLeasesLookup = new VaultLeasesLookup();
    AtomicBoolean lookupLeaseShouldReturn400 = new AtomicBoolean(false);
    VaultRenewLease vaultRenewLease = new VaultRenewLease();
    VaultConfigHolder vaultConfigHolder = new VaultConfigHolder().setVaultBootstrapConfig(config);
    VaultClient vaultClient = createVaultClient();
    VaultInternalSystemBackend vaultInternalSystemBackend = createSystemBackend();
    VaultInternalDatabaseSecretEngine vaultInternalDatabaseSecretEngine = createVaultInternalDatabaseSecretEngine();
    VaultAuthManager vaultAuthManager = new VaultAuthManager(vaultConfigHolder, vaultInternalSystemBackend,
            new VaultInternalAppRoleAuthMethod(),
            new VaultInternalKubernetesAuthMethod(),
            new VaultInternalUserpassAuthMethod(), new VaultInternalTokenAuthMethod());
    VaultDbManager vaultDbManager = new VaultDbManager(vaultConfigHolder, vaultAuthManager,
            vaultInternalSystemBackend, vaultInternalDatabaseSecretEngine);
    String mydbrole = "mydbrole";
    String mylease = "mylease";

    @AfterEach
    public void after() {
        vaultClient.close();
    }

    @Test
    public void getDynamicDbCredentials() {

        credentials.data = new VaultDatabaseCredentialsData();
        credentials.data.username = "bob";
        credentials.data.password = "sinclair1";
        credentials.leaseId = mylease;
        credentials.leaseDurationSecs = 10;
        credentials.renewable = true;

        vaultRenewLease.leaseId = mylease;
        vaultRenewLease.leaseDurationSecs = 10;
        vaultRenewLease.renewable = true;

        Map<String, String> properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
        assertEquals("bob", properties.get(USER_PROPERTY_NAME));
        assertEquals("sinclair1", properties.get(PASSWORD_PROPERTY_NAME));

        VaultDynamicDatabaseCredentials vaultDynamicDatabaseCredentials = vaultDbManager.credentialsCache.get(mydbrole);
        vaultDynamicDatabaseCredentials.created = EPOCH;
        vaultDynamicDatabaseCredentials.nowSupplier = () -> EPOCH.plusSeconds(1);
        credentials.data.password = "sinclair2";
        properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
        assertEquals("sinclair1", properties.get(PASSWORD_PROPERTY_NAME), "returned from cache");

        lookupLeaseShouldReturn400.set(true);
        properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
        assertEquals("sinclair2", properties.get(PASSWORD_PROPERTY_NAME), "fetched again because of 400");
        lookupLeaseShouldReturn400.set(false);

        vaultDynamicDatabaseCredentials = vaultDbManager.credentialsCache.get(mydbrole);
        vaultDynamicDatabaseCredentials.created = EPOCH;
        vaultDynamicDatabaseCredentials.password = "sinclair3"; // this will be extended with the current value in cache
        vaultDynamicDatabaseCredentials.nowSupplier = () -> EPOCH.plusSeconds(8); // within renew grace period
        properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
        assertEquals("sinclair3", properties.get(PASSWORD_PROPERTY_NAME), "extended");

        vaultDynamicDatabaseCredentials = vaultDbManager.credentialsCache.get(mydbrole);
        vaultDynamicDatabaseCredentials.created = EPOCH;
        vaultDynamicDatabaseCredentials.nowSupplier = () -> EPOCH.plusSeconds(12); // expired
        credentials.data.password = "sinclair4";
        properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
        assertEquals("sinclair4", properties.get(PASSWORD_PROPERTY_NAME), "recreated");

        vaultDynamicDatabaseCredentials = vaultDbManager.credentialsCache.get(mydbrole);
        vaultDynamicDatabaseCredentials.created = EPOCH;
        vaultDynamicDatabaseCredentials.nowSupplier = () -> EPOCH.plusSeconds(8); // within renew grace period
        vaultDynamicDatabaseCredentials.leaseDurationSecs = 2;
        credentials.data.password = "sinclair5";
        properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
        assertEquals("sinclair5", properties.get(PASSWORD_PROPERTY_NAME), "reaching max-ttl");
    }

    private VaultBootstrapConfig createConfig() {
        try {
            VaultBootstrapConfig config = new VaultBootstrapConfig();
            config.tls = new VaultTlsConfig();
            config.authentication = new VaultAuthenticationConfig();
            config.authentication.kubernetes = new VaultKubernetesAuthenticationConfig();
            config.authentication.appRole = new VaultAppRoleAuthenticationConfig();
            config.authentication.userpass = new VaultUserpassAuthenticationConfig();
            config.url = Optional.of(new URL("http://localhost:8200"));
            config.authentication.clientToken = Optional.of("123");
            config.authentication.clientTokenWrappingToken = Optional.empty();
            config.authentication.kubernetes.role = Optional.empty();
            config.authentication.appRole.roleId = Optional.empty();
            config.authentication.appRole.secretId = Optional.empty();
            config.authentication.appRole.secretIdWrappingToken = Optional.empty();
            config.authentication.userpass.username = Optional.empty();
            config.authentication.userpass.password = Optional.empty();
            config.authentication.userpass.passwordWrappingToken = Optional.empty();
            config.connectTimeout = Duration.ofSeconds(1);
            config.readTimeout = Duration.ofSeconds(1);
            config.nonProxyHosts = Optional.empty();
            config.tls.skipVerify = Optional.of(true);
            config.logConfidentialityLevel = LogConfidentialityLevel.LOW;
            config.renewGracePeriod = Duration.ofSeconds(3);
            return config;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private VaultClient createVaultClient() {
        VertxVaultClient vaultClient = new VertxVaultClient(vaultConfigHolder, tlsConfig);
        vaultClient.init();
        return vaultClient;
    }

    private VaultInternalDatabaseSecretEngine createVaultInternalDatabaseSecretEngine() {
        return new VaultInternalDatabaseSecretEngine() {
            @Override
            public VaultDatabaseCredentials generateCredentials(String token, String databaseCredentialsRole) {
                return credentials;
            }
        };
    }

    private VaultInternalSystemBackend createSystemBackend() {
        return new VaultInternalSystemBackend() {
            @Override
            public VaultLeasesLookup lookupLease(String token, String leaseId) {
                if (lookupLeaseShouldReturn400.get()) {
                    throw new VaultClientException(400, "400");
                }
                return vaultLeasesLookup;
            }

            @Override
            public VaultRenewLease renewLease(String token, String leaseId) {
                return vaultRenewLease;
            }
        };
    }
}
