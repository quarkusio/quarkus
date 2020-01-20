package io.quarkus.vault.runtime;

import static io.quarkus.vault.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.vault.CredentialsProvider.USER_PROPERTY_NAME;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.runtime.client.OkHttpVaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentialsData;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.config.VaultAppRoleAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultKubernetesAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;
import io.quarkus.vault.runtime.config.VaultUserpassAuthenticationConfig;

public class VaultDbManagerTest {

    VaultRuntimeConfig config = createConfig();
    VaultDatabaseCredentials credentials = new VaultDatabaseCredentials();
    VaultLeasesLookup vaultLeasesLookup = new VaultLeasesLookup();
    AtomicBoolean lookupLeaseShouldReturn400 = new AtomicBoolean(false);
    VaultRenewLease vaultRenewLease = new VaultRenewLease();
    OkHttpVaultClient vaultClient = createVaultClient();
    VaultAuthManager vaultAuthManager = new VaultAuthManager(vaultClient, config);
    VaultDbManager vaultDbManager = new VaultDbManager(vaultAuthManager, vaultClient, config);
    String mydbrole = "mydbrole";
    String mylease = "mylease";

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

        Properties properties = vaultDbManager.getDynamicDbCredentials(mydbrole);
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

    private VaultRuntimeConfig createConfig() {
        try {
            VaultRuntimeConfig config = new VaultRuntimeConfig();
            config.tls = new VaultTlsConfig();
            config.authentication = new VaultAuthenticationConfig();
            config.authentication.kubernetes = new VaultKubernetesAuthenticationConfig();
            config.authentication.appRole = new VaultAppRoleAuthenticationConfig();
            config.authentication.userpass = new VaultUserpassAuthenticationConfig();
            config.url = Optional.of(new URL("http://localhost:8200"));
            config.authentication.clientToken = Optional.of("123");
            config.authentication.kubernetes.role = Optional.empty();
            config.authentication.appRole.roleId = Optional.empty();
            config.authentication.appRole.secretId = Optional.empty();
            config.authentication.userpass.username = Optional.empty();
            config.authentication.userpass.password = Optional.empty();
            config.connectTimeout = Duration.ofSeconds(1);
            config.readTimeout = Duration.ofSeconds(1);
            config.tls.skipVerify = true;
            config.logConfidentialityLevel = LogConfidentialityLevel.LOW;
            config.renewGracePeriod = Duration.ofSeconds(3);
            return config;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private OkHttpVaultClient createVaultClient() {
        return new OkHttpVaultClient(config) {
            @Override
            public VaultDatabaseCredentials generateDatabaseCredentials(String token, String databaseCredentialsRole) {
                return credentials;
            }

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
