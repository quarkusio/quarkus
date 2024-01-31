package io.quarkus.oidc.client.runtime;

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

    final TokensHelper tokensHelper = new TokensHelper();

    @PostConstruct
    public void init() {
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

        initTokens();
    }

    protected void initTokens() {
        if (earlyTokenAcquisition) {
            tokensHelper.initTokens(oidcClient);
        }
    }

    public Uni<Tokens> getTokens() {
        final boolean forceNewTokens = isForceNewTokens();
        if (forceNewTokens) {
            final Optional<String> clientId = clientId();
            LOG.debugf("%s OidcClient will discard the current access and refresh tokens",
                    clientId.orElse(DEFAULT_OIDC_CLIENT_ID));
        }
        return tokensHelper.getTokens(oidcClient, forceNewTokens);
    }

    public Tokens awaitTokens() {
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
     * @return {@code true} if the OIDC client must acquire a new set of tokens, discarding
     *         previously obtained access and refresh tokens.
     */
    protected boolean isForceNewTokens() {
        return false;
    }
}
