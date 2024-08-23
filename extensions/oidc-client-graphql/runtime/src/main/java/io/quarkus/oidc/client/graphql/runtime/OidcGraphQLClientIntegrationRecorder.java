package io.quarkus.oidc.client.graphql.runtime;

import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.mutiny.Uni;

@Recorder
public class OidcGraphQLClientIntegrationRecorder {

    public void enhanceGraphQLClientConfigurationWithOidc(Map<String, String> configKeysToOidcClients,
            String defaultOidcClientName) {
        OidcClients oidcClients = Arc.container().instance(OidcClients.class).get();
        GraphQLClientsConfiguration configs = GraphQLClientsConfiguration.getInstance();
        configs.getClients().forEach((graphQLClientKey, value) -> {
            String oidcClient = configKeysToOidcClients.get(graphQLClientKey);
            if (oidcClient == null) {
                oidcClient = defaultOidcClientName;
            }
            Map<String, Uni<String>> dynamicHeaders = configs.getClient(graphQLClientKey).getDynamicHeaders();
            dynamicHeaders.put("Authorization", getToken(oidcClients, oidcClient));
        });
    }

    public Uni<String> getToken(OidcClients clients, String oidcClientId) {
        if (oidcClientId == null) {
            return clients.getClient()
                    .getTokens()
                    .map(Tokens::getAccessToken)
                    .map(token -> "Bearer " + token);
        } else {
            return clients.getClient(oidcClientId)
                    .getTokens()
                    .map(Tokens::getAccessToken)
                    .map(token -> "Bearer " + token);
        }
    }

}
