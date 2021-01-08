package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.auth.AbstractVaultAuthAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleGenerateNewSecretID;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultTokenCreate;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

/**
 * Handles authentication. Supports revocation and renewal.
 */
@Singleton
public class VaultAuthManager {

    private static final Logger log = Logger.getLogger(VaultAuthManager.class.getName());
    public static final String USERPASS_WRAPPING_TOKEN_PASSWORD_KEY = "password";

    private VaultClient vaultClient;
    private AtomicReference<VaultToken> loginCache = new AtomicReference<>(null);
    private Map<String, String> wrappedCache = new ConcurrentHashMap<>();
    private Semaphore unwrapSem = new Semaphore(1);
    private VaultConfigHolder vaultConfigHolder;

    VaultAuthManager(VaultConfigHolder vaultConfigHolder, VaultClient vaultClient) {
        this.vaultConfigHolder = vaultConfigHolder;
        this.vaultClient = vaultClient;
    }

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    public String getClientToken() {
        return getConfig().authentication.isDirectClientToken() ? getDirectClientToken() : login().clientToken;
    }

    private String getDirectClientToken() {

        Optional<String> clientTokenOption = getConfig().authentication.clientToken;
        if (clientTokenOption.isPresent()) {
            return clientTokenOption.get();
        }

        return unwrapWrappingTokenOnce("client token",
                getConfig().authentication.clientTokenWrappingToken.get(), unwrap -> unwrap.auth.clientToken,
                VaultTokenCreate.class);
    }

    private VaultToken login() {
        VaultToken vaultToken = login(loginCache.get());
        loginCache.set(vaultToken);
        return vaultToken;
    }

    public VaultToken login(VaultToken currentVaultToken) {

        VaultToken vaultToken = currentVaultToken;

        // check clientToken is still valid
        if (vaultToken != null) {
            vaultToken = validate(vaultToken);
        }

        // extend clientToken if necessary
        if (vaultToken != null && vaultToken.shouldExtend(getConfig().renewGracePeriod)) {
            vaultToken = extend(vaultToken.clientToken);
        }

        // create new clientToken if necessary
        if (vaultToken == null || vaultToken.isExpired() || vaultToken.expiresSoon(getConfig().renewGracePeriod)) {
            vaultToken = vaultLogin();
        }

        return vaultToken;
    }

    private VaultToken validate(VaultToken vaultToken) {
        try {
            vaultClient.lookupSelf(vaultToken.clientToken);
            return vaultToken;
        } catch (VaultClientException e) {
            if (e.getStatus() == 403) { // forbidden
                log.debug("login token " + vaultToken.clientToken + " has become invalid");
                return null;
            } else {
                throw e;
            }
        }
    }

    private VaultToken extend(String clientToken) {
        VaultRenewSelfAuth auth = vaultClient.renewSelf(clientToken, null).auth;
        VaultToken vaultToken = new VaultToken(auth.clientToken, auth.renewable, auth.leaseDurationSecs);
        sanityCheck(vaultToken);
        log.debug("extended login token: " + vaultToken.getConfidentialInfo(getConfig().logConfidentialityLevel));
        return vaultToken;
    }

    private VaultToken vaultLogin() {
        VaultToken vaultToken = login(getConfig().getAuthenticationType());
        sanityCheck(vaultToken);
        log.debug(
                "created new login token: " + vaultToken.getConfidentialInfo(getConfig().logConfidentialityLevel));
        return vaultToken;
    }

    private VaultToken login(VaultAuthenticationType type) {
        AbstractVaultAuthAuth<?> auth;
        if (type == KUBERNETES) {
            auth = loginKubernetes();
        } else if (type == USERPASS) {
            String username = getConfig().authentication.userpass.username.get();
            String password = getPassword();
            auth = vaultClient.loginUserPass(username, password).auth;
        } else if (type == APPROLE) {
            String roleId = getConfig().authentication.appRole.roleId.get();
            String secretId = getSecretId();
            auth = vaultClient.loginAppRole(roleId, secretId).auth;
        } else {
            throw new UnsupportedOperationException("unknown authType " + getConfig().getAuthenticationType());
        }

        return new VaultToken(auth.clientToken, auth.renewable, auth.leaseDurationSecs);
    }

    private String getSecretId() {

        Optional<String> secretIdOption = getConfig().authentication.appRole.secretId;
        if (secretIdOption.isPresent()) {
            return secretIdOption.get();
        }

        return unwrapWrappingTokenOnce("secret id",
                getConfig().authentication.appRole.secretIdWrappingToken.get(), unwrap -> unwrap.data.secretId,
                VaultAppRoleGenerateNewSecretID.class);
    }

    private String getPassword() {

        Optional<String> passwordOption = getConfig().authentication.userpass.password;
        if (passwordOption.isPresent()) {
            return passwordOption.get();
        }

        String wrappingToken = getConfig().authentication.userpass.passwordWrappingToken.get();
        if (getConfig().kvSecretEngineVersion == 1) {
            Function<VaultKvSecretV1, String> f = unwrap -> unwrap.data.get(USERPASS_WRAPPING_TOKEN_PASSWORD_KEY);
            return unwrapWrappingTokenOnce("password", wrappingToken, f, VaultKvSecretV1.class);
        } else {
            Function<VaultKvSecretV2, String> f = unwrap -> unwrap.data.data.get(USERPASS_WRAPPING_TOKEN_PASSWORD_KEY);
            return unwrapWrappingTokenOnce("password", wrappingToken, f, VaultKvSecretV2.class);
        }
    }

    private <T> String unwrapWrappingTokenOnce(String type, String wrappingToken,
            Function<T, String> f, Class<T> clazz) {

        // if the wrapped info is already in the cache, no need to go through semaphore acquisition
        String wrappedValue = wrappedCache.get(wrappingToken);
        if (wrappedValue != null) {
            return wrappedValue;
        }

        try {
            boolean success = unwrapSem.tryAcquire(1, 10, SECONDS);
            if (!success) {
                throw new RuntimeException("unable to enter critical section when unwrapping " + type);
            }
            try {
                // by the time we reach here, may be somebody has populated the cache
                wrappedValue = wrappedCache.get(wrappingToken);
                if (wrappedValue != null) {
                    return wrappedValue;
                }

                T unwrap;

                try {
                    unwrap = vaultClient.unwrap(wrappingToken, clazz);
                } catch (VaultClientException e) {
                    if (e.getStatus() == 400) {
                        String message = "wrapping token is not valid or does not exist; " +
                                "this means that the token has already expired " +
                                "(if so you can increase the ttl on the wrapping token) or " +
                                "has been consumed by somebody else " +
                                "(potentially indicating that the wrapping token has been stolen)";
                        throw new VaultException(message, e);
                    } else {
                        throw e;
                    }
                }

                wrappedValue = f.apply(unwrap);
                wrappedCache.put(wrappingToken, wrappedValue);
                String displayValue = getConfig().logConfidentialityLevel.maskWithTolerance(wrappedValue, LOW);
                log.debug("unwrapped " + type + ": " + displayValue);
                return wrappedValue;

            } finally {
                unwrapSem.release();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("unable to enter critical section when unwrapping " + type, e);
        }
    }

    private VaultKubernetesAuthAuth loginKubernetes() {
        String jwt = new String(read(getConfig().authentication.kubernetes.jwtTokenPath), StandardCharsets.UTF_8);
        log.debug("authenticate with jwt at: " + getConfig().authentication.kubernetes.jwtTokenPath + " => "
                + getConfig().logConfidentialityLevel.maskWithTolerance(jwt, LOW));
        return vaultClient.loginKubernetes(getConfig().authentication.kubernetes.role.get(), jwt).auth;
    }

    private byte[] read(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sanityCheck(VaultToken vaultToken) {
        vaultToken.leaseDurationSanityCheck("auth", getConfig().renewGracePeriod);
    }

}
