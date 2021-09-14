package io.quarkus.vault.runtime;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
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
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuthAuth;
import io.quarkus.vault.runtime.config.VaultAppRoleAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.quarkus.vault.runtime.config.VaultKubernetesAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;
import io.quarkus.vault.runtime.config.VaultUserpassAuthenticationConfig;

public class VaultAuthManagerTest {

    VaultBootstrapConfig config = createConfig();
    TlsConfig tlsConfig = new TlsConfig();
    AtomicBoolean lookupSelfShouldReturn403 = new AtomicBoolean(false);
    VaultConfigHolder vaultConfigHolder = new VaultConfigHolder().setVaultBootstrapConfig(config);
    VaultClient vaultClient = createVaultClient();
    VaultAuthManager vaultAuthManager = new VaultAuthManager(vaultConfigHolder,
            new VaultInternalSystemBackend(), new VaultInternalAppRoleAuthMethod(),
            new VaultInternalKubernetesAuthMethod(), createUserpassAuthMethod(),
            createTokenAuthMethod());
    VaultUserPassAuth vaultUserPassAuth = new VaultUserPassAuth();
    VaultLookupSelf vaultLookupSelf = new VaultLookupSelf();
    VaultRenewSelf vaultRenewSelf = new VaultRenewSelf();

    @AfterEach
    public void after() {
        vaultClient.close();
    }

    @Test
    public void login() {

        vaultUserPassAuth.auth = new VaultUserPassAuthAuth();
        vaultUserPassAuth.auth.clientToken = "1";
        vaultUserPassAuth.auth.renewable = true;
        vaultUserPassAuth.auth.leaseDurationSecs = 10;

        vaultLookupSelf.renewable = true;
        vaultLookupSelf.leaseDurationSecs = 10;

        vaultRenewSelf.auth = new VaultRenewSelfAuth();
        vaultRenewSelf.auth.renewable = true;
        vaultRenewSelf.auth.leaseDurationSecs = 10;

        VaultToken token = vaultAuthManager.login(null);
        assertEquals("1", token.clientToken);

        token.created = EPOCH;
        token.nowSupplier = () -> EPOCH.plusSeconds(2);
        token = vaultAuthManager.login(token);
        vaultUserPassAuth.auth.clientToken = "2";
        assertEquals("1", token.clientToken, "return from cache");

        token.created = EPOCH;
        token.nowSupplier = () -> EPOCH.plusSeconds(8);
        vaultRenewSelf.auth.clientToken = "3";
        token = vaultAuthManager.login(token);
        assertEquals("3", token.clientToken, "extended");

        token.created = EPOCH;
        token.nowSupplier = () -> EPOCH.plusSeconds(2);
        lookupSelfShouldReturn403.set(true);
        vaultUserPassAuth.auth.clientToken = "4";
        token = vaultAuthManager.login(token);
        assertEquals("4", token.clientToken, "invalid -> recreated");
        lookupSelfShouldReturn403.set(false);

        token.created = EPOCH;
        token.nowSupplier = () -> EPOCH.plusSeconds(12);
        vaultUserPassAuth.auth.clientToken = "5";
        token = vaultAuthManager.login(token);
        assertEquals("5", token.clientToken, "expired");

        token.created = EPOCH;
        token.nowSupplier = () -> EPOCH.plusSeconds(8);
        token.leaseDurationSecs = 2;
        vaultUserPassAuth.auth.clientToken = "6";
        token = vaultAuthManager.login(token);
        assertEquals("6", token.clientToken, "expires soon");
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
            config.authentication.clientToken = Optional.empty();
            config.authentication.clientTokenWrappingToken = Optional.empty();
            config.authentication.kubernetes.role = Optional.empty();
            config.authentication.appRole.roleId = Optional.empty();
            config.authentication.appRole.secretId = Optional.empty();
            config.authentication.appRole.secretIdWrappingToken = Optional.empty();
            config.authentication.userpass.username = Optional.of("bob");
            config.authentication.userpass.password = Optional.of("sinclair");
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

    private VaultInternalTokenAuthMethod createTokenAuthMethod() {
        return new VaultInternalTokenAuthMethod() {
            @Override
            public VaultLookupSelf lookupSelf(String token) {
                if (lookupSelfShouldReturn403.get()) {
                    throw new VaultClientException(403, "403");
                }
                return vaultLookupSelf;
            }

            @Override
            public VaultRenewSelf renewSelf(String token, String increment) {
                return vaultRenewSelf;
            }
        };
    }

    private VaultInternalUserpassAuthMethod createUserpassAuthMethod() {
        return new VaultInternalUserpassAuthMethod() {
            @Override
            public VaultUserPassAuth login(String user, String password) {
                return vaultUserPassAuth;
            }
        };
    }
}
