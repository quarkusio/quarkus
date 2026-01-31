package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

/**
 * {@link OidcClient} that wraps a lazily initialized OIDC client whose OIDC metadata discovery failed on the application
 * startup.
 * If the discovery fails when any of the wrapped {@link OidcClient} methods is invoked, we return a {@link Uni} failure.
 */
final class DeferredOidcClient implements OidcClient {

    private static final Logger LOG = Logger.getLogger(DeferredOidcClient.class);
    private final Uni<OidcClient> deferredOidcClient;
    private final String oidcClientId;
    private volatile OidcClient resolvedOidcClient;

    DeferredOidcClient(Uni<OidcClient> deferredOidcClient, String oidcClientId) {
        this.deferredOidcClient = deferredOidcClient;
        this.resolvedOidcClient = null;
        this.oidcClientId = oidcClientId;
    }

    @Override
    public Uni<Tokens> getTokens() {
        return runWithOidcClient(OidcClient::getTokens);
    }

    @Override
    public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
        return runWithOidcClient(oidcClient -> oidcClient.getTokens(additionalGrantParameters));
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken) {
        return runWithOidcClient(oidcClient -> oidcClient.refreshTokens(refreshToken));
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
        return runWithOidcClient(oidcClient -> oidcClient.refreshTokens(refreshToken, additionalGrantParameters));
    }

    @Override
    public Uni<Boolean> revokeAccessToken(String accessToken) {
        return runWithOidcClient(oidcClient -> oidcClient.revokeAccessToken(accessToken));
    }

    @Override
    public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
        return runWithOidcClient(oidcClient -> oidcClient.revokeAccessToken(accessToken, additionalParameters));
    }

    @Override
    public void close() throws IOException {
        if (resolvedOidcClient != null) {
            resolvedOidcClient.close();
        }
    }

    OidcClient getResolvedOidcClient() {
        return resolvedOidcClient;
    }

    private <T> Uni<T> runWithOidcClient(Function<OidcClient, Uni<T>> action) {
        if (resolvedOidcClient != null) {
            return action.apply(resolvedOidcClient);
        }

        return deferredOidcClient.flatMap(oidcClient -> {
            DeferredOidcClient.this.resolvedOidcClient = oidcClient;
            LOG.debugf("OIDC client '%s' metadata discovery succeeded", oidcClientId);
            return action.apply(oidcClient);
        });
    }
}
