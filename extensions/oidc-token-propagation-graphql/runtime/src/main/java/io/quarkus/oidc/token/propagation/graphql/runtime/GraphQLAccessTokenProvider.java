package io.quarkus.oidc.token.propagation.graphql.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ContextNotActiveException;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.core.StartContext;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.client.runtime.OidcClientsConfig;
import io.quarkus.oidc.client.runtime.TokensHelper;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.credential.TokenCredential;
import io.smallrye.config.Config;
import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Provides the {@code Authorization} header value for a GraphQL client by propagating the access token of the current
 * {@link TokenCredential}. If the token exchange is activated, the access token is exchanged before it is propagated.
 * <p>
 * The header value is resolved for each subscription, that is for each GraphQL client request.
 * This class does not use the {@link Uni} builders, because they capture the context of the thread which creates
 * the {@link Uni} during the application startup, while the access token must be resolved in the context
 * of the thread which sends the GraphQL client request.
 */
public final class GraphQLAccessTokenProvider extends AbstractUni<String> {

    private static final Logger LOG = Logger.getLogger(GraphQLAccessTokenProvider.class);
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_SCHEME_WITH_SPACE = "Bearer ";

    private final String graphQLClientKey;
    // null if the access token is propagated without being exchanged, empty for the default OIDC client
    private final String tokenExchangeOidcClient;
    private final Function<Tokens, String> exchangedTokensToAuthorizationHeader;
    private volatile TokenExchange tokenExchange;

    private GraphQLAccessTokenProvider(String graphQLClientKey, String tokenExchangeOidcClient) {
        this.graphQLClientKey = graphQLClientKey;
        this.tokenExchangeOidcClient = tokenExchangeOidcClient;
        this.exchangedTokensToAuthorizationHeader = new Function<Tokens, String>() {
            @Override
            public String apply(Tokens tokens) {
                if (tokens.getAccessToken() == null) {
                    throw new IllegalStateException(
                            "GraphQL client '%s' cannot propagate the exchanged access token: the %s OIDC client did not return it"
                                    .formatted(graphQLClientKey, oidcClientDescription()));
                }
                return BEARER_SCHEME_WITH_SPACE + tokens.getAccessToken();
            }
        };
    }

    /**
     * Makes the GraphQL clients propagate the current access token as the {@code Authorization} header value.
     *
     * @param startContext start context
     * @param graphQLClientKeys configuration keys of the GraphQL clients which propagate the access token
     * @param tokenExchangeOidcClients configuration keys of the GraphQL clients which exchange the access token before it is
     *        propagated, mapped to the name of the OIDC client which exchanges the token; the default OIDC client name is empty
     */
    public static void propagateAccessTokens(StartContext startContext, Set<String> graphQLClientKeys,
            Map<String, String> tokenExchangeOidcClients) {
        GraphQLClientsConfiguration clientsConfiguration = GraphQLClientsConfiguration.getInstance();
        for (String graphQLClientKey : graphQLClientKeys) {
            GraphQLClientConfiguration clientConfiguration = clientsConfiguration.getClient(graphQLClientKey);
            // the GraphQL clients only add the headers to the requests sent over HTTP
            if (Boolean.TRUE.equals(clientConfiguration.getExecuteSingleOperationsOverWebsocket())) {
                throw new ConfigurationException(
                        """
                                GraphQL client '%1$s' is annotated with the @AccessToken annotation, but it executes queries and mutations \
                                over WebSocket because the 'quarkus.smallrye-graphql-client."%1$s".execute-single-result-operations-over-websocket' \
                                configuration property is enabled. The access token can only be propagated over HTTP."""
                                .formatted(graphQLClientKey));
            }
        }
        Map<String, GraphQLAccessTokenProvider> providers = new HashMap<>();
        for (String graphQLClientKey : graphQLClientKeys) {
            GraphQLAccessTokenProvider provider = new GraphQLAccessTokenProvider(graphQLClientKey,
                    tokenExchangeOidcClients.get(graphQLClientKey));
            clientsConfiguration.getClient(graphQLClientKey).getDynamicHeaders().put(AUTHORIZATION, provider);
            providers.put(graphQLClientKey, provider);
        }
        // the GraphQL clients configuration can outlive the application, for example in the dev mode
        startContext.onStop(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, GraphQLAccessTokenProvider> provider : providers.entrySet()) {
                    GraphQLClientConfiguration clientConfiguration = clientsConfiguration.getClients().get(provider.getKey());
                    if (clientConfiguration != null && clientConfiguration.getDynamicHeaders() != null) {
                        // the headers are replaced, because the GraphQL clients which may still be in use share them
                        Map<String, Uni<String>> dynamicHeaders = new HashMap<>(clientConfiguration.getDynamicHeaders());
                        if (dynamicHeaders.remove(AUTHORIZATION, provider.getValue())) {
                            clientConfiguration.setDynamicHeaders(dynamicHeaders);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void subscribe(UniSubscriber<? super String> subscriber) {
        Uni<String> authorizationHeader;
        try {
            String accessToken = getAccessToken();
            if (tokenExchangeOidcClient == null) {
                authorizationHeader = Uni.createFrom().item(BEARER_SCHEME_WITH_SPACE + accessToken);
            } else {
                authorizationHeader = exchangeToken(accessToken);
            }
        } catch (RuntimeException e) {
            authorizationHeader = Uni.createFrom().failure(e);
        }
        AbstractUni.subscribe(authorizationHeader, subscriber);
    }

    private String getAccessToken() {
        ArcContainer container = Arc.container();
        if (container == null) {
            throw accessTokenNotAvailable("the CDI container is not available, the application may be shutting down", null);
        }
        try (InstanceHandle<TokenCredential> tokenCredential = container.instance(TokenCredential.class)) {
            if (!tokenCredential.isAvailable()) {
                throw accessTokenNotAvailable("there is either no TokenCredential bean or more than one such bean,"
                        + " the bean is provided by the extensions like quarkus-oidc or quarkus-smallrye-jwt", null);
            }
            String accessToken;
            try {
                accessToken = tokenCredential.get().getToken();
            } catch (ContextNotActiveException e) {
                throw accessTokenNotAvailable("the CDI request context is not active,"
                        + " the access token can only be propagated while a request is being processed", e);
            }
            if (accessToken == null) {
                throw accessTokenNotAvailable("the current TokenCredential has no token,"
                        + " the current request is not authenticated with a token", null);
            }
            return accessToken;
        }
    }

    private IllegalStateException accessTokenNotAvailable(String reason, Throwable cause) {
        String message = "No access token is available to propagate to the GraphQL client '%s': %s"
                .formatted(graphQLClientKey, reason);
        LOG.debug(message);
        return new IllegalStateException(message, cause);
    }

    private Uni<String> exchangeToken(String accessToken) {
        TokenExchange tokenExchange = getTokenExchange();
        // the exchanged tokens are cached for each access token
        return tokenExchange.tokensHelper()
                .getTokens(tokenExchange.oidcClient(), Map.of(tokenExchange.tokenProperty(), accessToken), false)
                .map(exchangedTokensToAuthorizationHeader);
    }

    private TokenExchange getTokenExchange() {
        TokenExchange tokenExchange = this.tokenExchange;
        if (tokenExchange == null) {
            synchronized (this) {
                tokenExchange = this.tokenExchange;
                if (tokenExchange == null) {
                    tokenExchange = createTokenExchange();
                    this.tokenExchange = tokenExchange;
                }
            }
        }
        return tokenExchange;
    }

    private TokenExchange createTokenExchange() {
        boolean defaultOidcClient = tokenExchangeOidcClient.isEmpty();
        OidcClients oidcClients = Arc.requireContainer().instance(OidcClients.class).get();
        OidcClient oidcClient = null;
        if (oidcClients != null) {
            oidcClient = defaultOidcClient ? oidcClients.getClient() : oidcClients.getClient(tokenExchangeOidcClient);
        }
        if (oidcClient == null) {
            throw new ConfigurationException(
                    "GraphQL client '%s' requires the %s OIDC client to exchange the access token, but there is no such OIDC client"
                            .formatted(graphQLClientKey, oidcClientDescription()));
        }
        OidcClientsConfig oidcClientsConfig = Config.get().getConfigMapping(OidcClientsConfig.class);
        OidcClientConfig oidcClientConfig = defaultOidcClient ? OidcClientsConfig.getDefaultClient(oidcClientsConfig)
                : oidcClientsConfig.namedClients().get(tokenExchangeOidcClient);
        Grant.Type grantType = oidcClientConfig.grant().type();
        final String tokenProperty;
        if (grantType == Grant.Type.EXCHANGE) {
            tokenProperty = OidcConstants.EXCHANGE_GRANT_SUBJECT_TOKEN;
        } else if (grantType == Grant.Type.JWT) {
            tokenProperty = OidcConstants.JWT_BEARER_GRANT_ASSERTION;
        } else {
            throw new ConfigurationException(
                    "GraphQL client '%s' requires the %s OIDC client to exchange the access token, but this OIDC client is configured to use the %s grant type, only the token exchange and JWT bearer grant types can exchange tokens"
                            .formatted(graphQLClientKey, oidcClientDescription(), grantType.getGrantType()));
        }
        return new TokenExchange(oidcClient, tokenProperty, new TokensHelper(oidcClientConfig.tokenCacheMaxSize()));
    }

    private String oidcClientDescription() {
        return tokenExchangeOidcClient.isEmpty() ? "default" : "'" + tokenExchangeOidcClient + "'";
    }

    private record TokenExchange(OidcClient oidcClient, String tokenProperty, TokensHelper tokensHelper) {
    }
}
