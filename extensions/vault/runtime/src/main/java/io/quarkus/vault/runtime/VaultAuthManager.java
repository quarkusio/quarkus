package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.auth.AbstractVaultAuthAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfAuth;
import io.quarkus.vault.runtime.client.dto.sys.VaultUnwrapAuth;
import io.quarkus.vault.runtime.client.dto.sys.VaultUnwrapResult;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

/**
 * Handles authentication. Supports revocation and renewal.
 */
public class VaultAuthManager {

    private static final Logger log = Logger.getLogger(VaultAuthManager.class.getName());

    private VaultRuntimeConfig serverConfig;
    private VaultClient vaultClient;
    private AtomicReference<VaultToken> auth = new AtomicReference<>(null);

    public VaultAuthManager(VaultClient vaultClient, VaultRuntimeConfig serverConfig) {
        this.vaultClient = vaultClient;
        this.serverConfig = serverConfig;
    }

    public String getClientToken() {
        return serverConfig.authentication.clientToken.orElseGet(() -> login().clientToken);
    }

    private VaultToken login() {
        VaultAuthManager service = new VaultAuthManager(vaultClient, serverConfig);
        VaultToken vaultToken = service.login(auth.get());
        auth.set(vaultToken);
        return vaultToken;
    }

    public VaultToken login(VaultToken currentVaultToken) {

        VaultToken vaultToken = currentVaultToken;

        // check clientToken is still valid
        if (vaultToken != null) {
            vaultToken = validate(vaultToken);
        }

        // extend clientToken if necessary
        if (vaultToken != null && vaultToken.shouldExtend(serverConfig.renewGracePeriod)) {
            vaultToken = extend(vaultToken.clientToken);
        }

        // create new clientToken if necessary
        if (vaultToken == null || vaultToken.isExpired() || vaultToken.expiresSoon(serverConfig.renewGracePeriod)) {
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
        log.debug("extended login token: " + vaultToken.getConfidentialInfo(serverConfig.logConfidentialityLevel));
        return vaultToken;
    }

    private VaultToken vaultLogin() {
        VaultToken vaultToken = login(serverConfig.getAuthenticationType());
        sanityCheck(vaultToken);
        log.debug("created new login token: " + vaultToken.getConfidentialInfo(serverConfig.logConfidentialityLevel));

        return vaultToken;
    }

    private VaultToken login(VaultAuthenticationType type) {
        AbstractVaultAuthAuth<?> auth;

        switch (type) {
            case KUBERNETES: auth = loginKubernetes(); break;
            case USERPASS: auth = loginUserPass(); break;
            case APPROLE: auth = loginAppRole(); break;
            default: throw new UnsupportedOperationException("unknown authType " + serverConfig.getAuthenticationType());
        }

        return new VaultToken(auth.clientToken, auth.renewable, auth.leaseDurationSecs);
    }

    private AbstractVaultAuthAuth<?> loginAppRole() {
        final AbstractVaultAuthAuth<?> auth;
        String roleId = serverConfig.authentication.appRole.roleId.get();
        String secretId = unwrapTokenOrPlainToken(serverConfig.authentication.appRole.secretIdWrapped,
                                                    serverConfig.authentication.appRole.secretId.get());

        auth = vaultClient.loginAppRole(roleId, secretId).auth;
        return auth;
    }

    private AbstractVaultAuthAuth<?> loginUserPass() {
        final AbstractVaultAuthAuth<?> auth;
        String username = serverConfig.authentication.userpass.username.get();
        String password = unwrapTokenOrPlainToken(serverConfig.authentication.userpass.passwordWrapped,
                                                    serverConfig.authentication.userpass.password.get());

        auth = vaultClient.loginUserPass(username, password).auth;
        return auth;
    }

    private VaultKubernetesAuthAuth loginKubernetes() {
        String jwt = new String(read(serverConfig.authentication.kubernetes.jwtTokenPath), StandardCharsets.UTF_8);
        log.debug("authenticate with jwt at: " + serverConfig.authentication.kubernetes.jwtTokenPath + " => "
                + serverConfig.logConfidentialityLevel.maskWithTolerance(jwt, LOW));
        return vaultClient.loginKubernetes(serverConfig.authentication.kubernetes.role.get(), jwt).auth;
    }

    private byte[] read(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sanityCheck(VaultToken vaultToken) {
        vaultToken.leaseDurationSanityCheck("auth", serverConfig.renewGracePeriod);
    }

    private String unwrapTokenOrPlainToken(Optional<String> wrappedToken, String plainToken) {
        return wrappedToken.flatMap(wrapped -> Optional.of(vaultClient.unwrap(wrapped).auth.clientToken))
            .orElse(plainToken);
    }

}
