package io.quarkus.oidc.token.propagation.graphql.runtime.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ContextNotActiveException;

import org.jboss.logging.Logger;

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

    private final Supplier<InstanceHandle<TokenCredential>> tokenCredentials;
    private final String graphQLClientKey;
    // null if the access token is propagated without being exchanged
    private final TokenExchange tokenExchange;

    private GraphQLAccessTokenProvider(Supplier<InstanceHandle<TokenCredential>> tokenCredentials, String graphQLClientKey,
            TokenExchange tokenExchange) {
        this.tokenCredentials = tokenCredentials;
        this.graphQLClientKey = graphQLClientKey;
        this.tokenExchange = tokenExchange;
    }

    /**
     * Makes the GraphQL clients propagate the current access token as the {@code Authorization} header value.
     *
     * @param startContext start context
     * @param container CDI container
     * @param graphQLClientKeys configuration keys of the GraphQL clients which propagate the access token
     * @param tokenExchangeOidcClients configuration keys of the GraphQL clients which exchange the access token before it is
     *        propagated, mapped to the name of the OIDC client which exchanges the token; the default OIDC client name is empty
     */
    public static void propagateAccessTokens(StartContext startContext, ArcContainer container, Set<String> graphQLClientKeys,
            Map<String, String> tokenExchangeOidcClients) {
        Supplier<InstanceHandle<TokenCredential>> tokenCredentials = container.beanInstanceSupplier(TokenCredential.class);
        if (tokenCredentials == null) {
            throw new ConfigurationException(
                    """
                            GraphQL clients annotated with the @AccessToken annotation propagate the token of the TokenCredential bean, \
                            but there is no such bean. The bean is provided by the extensions like quarkus-oidc or quarkus-smallrye-jwt.""");
        }
        GraphQLClientsConfiguration clientsConfiguration = GraphQLClientsConfiguration.getInstance();
        Map<String, GraphQLAccessTokenProvider> providers = new HashMap<>();
        // the GraphQL clients configuration can outlive the application, for example in the dev mode;
        // the stop handler is registered first, so that it also runs when a GraphQL client is rejected below
        startContext.onStop(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, GraphQLAccessTokenProvider> provider : providers.entrySet()) {
                    // the configuration is looked up again, because it is replaced whenever a GraphQL client is created
                    GraphQLClientConfiguration clientConfiguration = clientsConfiguration.getClient(provider.getKey());
                    // the headers are replaced, because the GraphQL clients which may still be in use share them
                    Map<String, Uni<String>> dynamicHeaders = new HashMap<>(clientConfiguration.getDynamicHeaders());
                    if (dynamicHeaders.remove(AUTHORIZATION, provider.getValue())) {
                        clientConfiguration.setDynamicHeaders(dynamicHeaders);
                    }
                }
            }
        });
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
            validateNoConfiguredAuthorizationHeader(graphQLClientKey, clientConfiguration);
            String tokenExchangeOidcClient = tokenExchangeOidcClients.get(graphQLClientKey);
            TokenExchange tokenExchange = tokenExchangeOidcClient == null ? null
                    : createTokenExchange(container, graphQLClientKey, tokenExchangeOidcClient);
            GraphQLAccessTokenProvider provider = new GraphQLAccessTokenProvider(tokenCredentials, graphQLClientKey,
                    tokenExchange);
            clientConfiguration.getDynamicHeaders().put(AUTHORIZATION, provider);
            providers.put(graphQLClientKey, provider);
        }
    }

    /**
     * The GraphQL clients send the propagated access token together with the {@code Authorization} header they already have.
     */
    private static void validateNoConfiguredAuthorizationHeader(String graphQLClientKey,
            GraphQLClientConfiguration clientConfiguration) {
        Map<String, String> headers = clientConfiguration.getHeaders();
        if (headers != null) {
            for (String headerName : headers.keySet()) {
                if (AUTHORIZATION.equalsIgnoreCase(headerName)) {
                    throw new ConfigurationException(
                            """
                                    GraphQL client '%1$s' is annotated with the @AccessToken annotation, but the \
                                    'quarkus.smallrye-graphql-client."%1$s".header."%2$s"' configuration property also sets \
                                    the 'Authorization' header. Only one of them can be used on the same GraphQL client."""
                                    .formatted(graphQLClientKey, headerName));
                }
            }
        }
        // the GraphQL clients use these properties for the HTTP Basic authentication
        String usernameProperty = graphQLClientKey + "/mp-graphql/username";
        String passwordProperty = graphQLClientKey + "/mp-graphql/password";
        Config config = Config.get();
        if (config.getOptionalValue(usernameProperty, String.class).isPresent()
                && config.getOptionalValue(passwordProperty, String.class).isPresent()) {
            throw new ConfigurationException(
                    """
                            GraphQL client '%s' is annotated with the @AccessToken annotation, but the '%s' and '%s' \
                            configuration properties also set the 'Authorization' header. \
                            Only one of them can be used on the same GraphQL client."""
                            .formatted(graphQLClientKey, usernameProperty, passwordProperty));
        }
    }

    @Override
    public void subscribe(UniSubscriber<? super String> subscriber) {
        Uni<String> authorizationHeader;
        try {
            String accessToken = getAccessToken();
            if (tokenExchange == null) {
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
        try (InstanceHandle<TokenCredential> tokenCredential = tokenCredentials.get()) {
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
        // the exchanged tokens are cached for each access token
        return tokenExchange.tokensHelper()
                .getTokens(tokenExchange.oidcClient(), Map.of(tokenExchange.tokenProperty(), accessToken), false)
                .map(tokens -> toAuthorizationHeader(tokens, graphQLClientKey, tokenExchange.oidcClientName()));
    }

    private static String toAuthorizationHeader(Tokens exchangedTokens, String graphQLClientKey, String oidcClientName) {
        if (exchangedTokens.getAccessToken() == null) {
            throw new IllegalStateException(
                    "GraphQL client '%s' cannot propagate the exchanged access token: the %s OIDC client did not return it"
                            .formatted(graphQLClientKey, oidcClientDescription(oidcClientName)));
        }
        return BEARER_SCHEME_WITH_SPACE + exchangedTokens.getAccessToken();
    }

    /**
     * @param oidcClientName name of the OIDC client which exchanges the access token, empty for the default OIDC client
     */
    private static TokenExchange createTokenExchange(ArcContainer container, String graphQLClientKey,
            String oidcClientName) {
        boolean defaultOidcClient = oidcClientName.isEmpty();
        OidcClients oidcClients = container.instance(OidcClients.class).get();
        OidcClient oidcClient = null;
        if (oidcClients != null) {
            oidcClient = defaultOidcClient ? oidcClients.getClient() : oidcClients.getClient(oidcClientName);
        }
        if (oidcClient == null) {
            throw new ConfigurationException(
                    "GraphQL client '%s' requires the %s OIDC client to exchange the access token, but there is no such OIDC client"
                            .formatted(graphQLClientKey, oidcClientDescription(oidcClientName)));
        }
        OidcClientsConfig oidcClientsConfig = Config.get().getConfigMapping(OidcClientsConfig.class);
        OidcClientConfig oidcClientConfig = defaultOidcClient ? OidcClientsConfig.getDefaultClient(oidcClientsConfig)
                : oidcClientsConfig.namedClients().get(oidcClientName);
        Grant.Type grantType = oidcClientConfig.grant().type();
        final String tokenProperty;
        if (grantType == Grant.Type.EXCHANGE) {
            tokenProperty = OidcConstants.EXCHANGE_GRANT_SUBJECT_TOKEN;
        } else if (grantType == Grant.Type.JWT) {
            tokenProperty = OidcConstants.JWT_BEARER_GRANT_ASSERTION;
        } else {
            throw new ConfigurationException(
                    "GraphQL client '%s' requires the %s OIDC client to exchange the access token, but this OIDC client is configured to use the %s grant type, only the token exchange and JWT bearer grant types can exchange tokens"
                            .formatted(graphQLClientKey, oidcClientDescription(oidcClientName),
                                    grantType.getGrantType()));
        }
        return new TokenExchange(oidcClient, oidcClientName, tokenProperty,
                new TokensHelper(oidcClientConfig.tokenCacheMaxSize()));
    }

    private static String oidcClientDescription(String oidcClientName) {
        return oidcClientName.isEmpty() ? "default" : "'" + oidcClientName + "'";
    }

    private record TokenExchange(OidcClient oidcClient, String oidcClientName, String tokenProperty,
            TokensHelper tokensHelper) {
    }
}
