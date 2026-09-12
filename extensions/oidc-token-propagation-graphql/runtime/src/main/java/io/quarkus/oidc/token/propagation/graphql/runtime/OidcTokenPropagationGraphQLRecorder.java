package io.quarkus.oidc.token.propagation.graphql.runtime;

import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;

@Recorder
public class OidcTokenPropagationGraphQLRecorder {

    private static final Logger LOG = Logger.getLogger(OidcTokenPropagationGraphQLRecorder.class);
    private static final String AUTHORIZATION = "Authorization";

    /**
     * Installs an Authorization dynamic header that propagates the current access token for every
     * GraphQL client whose config key is in {@code propagateConfigKeys}. Keys also present in
     * {@code configKeyToExchangeOidcClient} additionally exchange the token via the mapped OIDC
     * client name ({@code ""} = default client).
     */
    public void enhanceGraphQLClients(Set<String> propagateConfigKeys,
            Map<String, String> configKeyToExchangeOidcClient) {
        GraphQLClientsConfiguration configs = GraphQLClientsConfiguration.getInstance();
        for (String configKey : propagateConfigKeys) {
            GraphQLClientConfiguration clientConfig = configs.getClient(configKey);
            if (clientConfig == null) {
                LOG.warnf("GraphQL client with config key '%s' is annotated with @AccessToken but no matching "
                        + "GraphQL client configuration was found; skipping token propagation.", configKey);
                continue;
            }
            boolean exchange = configKeyToExchangeOidcClient.containsKey(configKey);
            String oidcClientName = configKeyToExchangeOidcClient.get(configKey);
            GraphQLAccessTokenProvider provider = new GraphQLAccessTokenProvider(exchange, oidcClientName);
            clientConfig.getDynamicHeaders().put(AUTHORIZATION, provider.getAuthorizationHeader());
        }
    }
}
