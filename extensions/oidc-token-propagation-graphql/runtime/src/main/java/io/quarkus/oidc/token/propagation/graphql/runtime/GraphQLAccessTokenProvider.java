package io.quarkus.oidc.token.propagation.graphql.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.credential.TokenCredential;
import io.smallrye.mutiny.Uni;

/**
 * Supplies the {@code Authorization} header value for a GraphQL typesafe client by propagating the
 * current {@link TokenCredential}, optionally exchanging it through a named {@link OidcClient}.
 */
public class GraphQLAccessTokenProvider {

    private static final Logger LOG = Logger.getLogger(GraphQLAccessTokenProvider.class);
    private static final String BEARER = "Bearer ";

    private final boolean exchange;
    // "" or null means the default OIDC client
    private final String oidcClientName;

    private volatile OidcClient exchangeClient;
    private volatile String exchangeProperty;

    public GraphQLAccessTokenProvider(boolean exchange, String oidcClientName) {
        this.exchange = exchange;
        this.oidcClientName = oidcClientName;
    }

    /**
     * @return a deferred Uni re-evaluated on every GraphQL request.
     */
    public Uni<String> getAuthorizationHeader() {
        return Uni.createFrom().deferred(() -> {
            String token = currentAccessToken();
            if (token == null) {
                LOG.debug("No access token available in the current context, failing the GraphQL request");
                return Uni.createFrom().failure(
                        new IllegalStateException("No access token is available to propagate to the GraphQL client"));
            }
            if (!exchange) {
                return Uni.createFrom().item(BEARER + token);
            }
            initExchangeClient();
            return exchangeClient.getTokens(Collections.singletonMap(exchangeProperty, token))
                    .map(tokens -> BEARER + tokens.getAccessToken());
        });
    }

    private static String currentAccessToken() {
        if (Arc.container() == null) {
            return null;
        }
        try (InstanceHandle<TokenCredential> handle = Arc.container().instance(TokenCredential.class)) {
            if (!handle.isAvailable()) {
                return null;
            }
            TokenCredential credential = handle.get();
            return credential != null ? credential.getToken() : null;
        }
    }

    private void initExchangeClient() {
        if (exchangeClient != null) {
            return;
        }
        synchronized (this) {
            if (exchangeClient != null) {
                return;
            }
            OidcClients clients = Arc.container().instance(OidcClients.class).get();
            boolean named = oidcClientName != null && !oidcClientName.isEmpty();
            OidcClient client = named ? clients.getClient(oidcClientName) : clients.getClient();
            Grant.Type grantType = ConfigProvider.getConfig().getValue(
                    "quarkus.oidc-client." + (named ? oidcClientName + "." : "") + "grant.type",
                    Grant.Type.class);
            if (grantType == Grant.Type.EXCHANGE) {
                exchangeProperty = OidcConstants.EXCHANGE_GRANT_SUBJECT_TOKEN;
            } else if (grantType == Grant.Type.JWT) {
                exchangeProperty = OidcConstants.JWT_BEARER_GRANT_ASSERTION;
            } else {
                throw new ConfigurationException("Token exchange is required but OIDC client is configured "
                        + "to use the " + grantType.getGrantType() + " grantType");
            }
            exchangeClient = client;
        }
    }
}
