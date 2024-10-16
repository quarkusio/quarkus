package io.quarkus.oidc.client.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

public abstract class AbstractTokensProducer {
    private static final Logger LOG = Logger.getLogger(AbstractTokensProducer.class);
    private static final String DEFAULT_OIDC_CLIENT_ID = "Default";
    private OidcClient oidcClient;

    protected boolean earlyTokenAcquisition = true;

    @Inject
    public OidcClientsConfig oidcClientsConfig;
    @Inject
    public OidcClientBuildTimeConfig oidcClientBuildTimeConfig;

    final TokensHelper tokensHelper = new TokensHelper();

    @PostConstruct
    public void init() {
        if (isClientFeatureDisabled()) {
            LOG.debug("OIDC client is disabled with `quarkus.oidc-client.enabled=false`,"
                    + " skipping the token producer initialization");
            return;
        }
        Optional<OidcClient> initializedClient = client();
        if (initializedClient.isEmpty()) {
            Optional<String> clientId = Objects.requireNonNull(clientId(), "clientId must not be null");
            OidcClients oidcClients = Arc.container().instance(OidcClients.class).get();
            if (clientId.isPresent()) {
                // static named OidcClient
                oidcClient = Objects.requireNonNull(oidcClients.getClient(clientId.get()), "Unknown client");
                earlyTokenAcquisition = oidcClientsConfig.namedClients.get(clientId.get()).earlyTokensAcquisition;
            } else {
                // default OidcClient
                earlyTokenAcquisition = oidcClientsConfig.defaultClient.earlyTokensAcquisition;
                oidcClient = oidcClients.getClient();
            }
        } else {
            oidcClient = initializedClient.get();
        }

        initTokens();
    }

    protected boolean isClientFeatureDisabled() {
        return !oidcClientBuildTimeConfig.enabled;
    }

    protected void initTokens() {
        if (isClientFeatureDisabled()) {
            throw new IllegalStateException("OIDC client feature is disabled with `quarkus.oidc-client.enabled=false`"
                    + " but the initTokens() method is called.");
        }
        if (earlyTokenAcquisition) {
            tokensHelper.initTokens(oidcClient, additionalParameters());
        }
    }

    public Uni<Tokens> getTokens() {
        if (isClientFeatureDisabled()) {
            throw new IllegalStateException("OIDC client feature is disabled with `quarkus.oidc-client.enabled=false`"
                    + " but the getTokens() method is called.");
        }
        final boolean forceNewTokens = isForceNewTokens();
        if (forceNewTokens) {
            final Optional<String> clientId = clientId();
            LOG.debugf("%s OidcClient will discard the current access and refresh tokens",
                    clientId.orElse(DEFAULT_OIDC_CLIENT_ID));
        }
        return tokensHelper.getTokens(oidcClient, additionalParameters(), forceNewTokens);
    }

    public Tokens awaitTokens() {
        if (isClientFeatureDisabled()) {
            throw new IllegalStateException("OIDC client feature is disabled with `quarkus.oidc-client.enabled=false`.");
        }
        return getTokens().await().indefinitely();
    }

    /**
     * @return optional ID of OIDC client to use for token acquisition.
     *         Defaults to default OIDC client when {@link Optional#empty() empty}.
     */
    protected Optional<String> clientId() {
        return Optional.empty();
    }

    /**
     * @return Initialized OidcClient.
     */
    protected Optional<OidcClient> client() {
        return Optional.empty();
    }

    /**
     * @return {@code true} if the OIDC client must acquire a new set of tokens, discarding
     *         previously obtained access and refresh tokens.
     */
    protected boolean isForceNewTokens() {
        return false;
    }

    /**
     * @return Additional parameters which will be used during the token acquisition or refresh methods.
     */
    protected Map<String, String> additionalParameters() {
        return Map.of();
    }
}
