package io.quarkus.oidc.client.graphql.runtime;

import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.mutiny.Uni;

@Recorder
public class OidcGraphQLClientIntegrationRecorder {

    public void enhanceGraphQLClientConfigurationWithOidc(Map<String, String> configKeysToOidcClients,
            String defaultOidcClientName,
            Map<String, String> oidcClientToTokenProducerName) {
        var container = Arc.requireContainer();
        GraphQLClientsConfiguration configs = GraphQLClientsConfiguration.getInstance();
        configs.getClients().forEach((graphQLClientKey, value) -> {
            String oidcClient = configKeysToOidcClients.get(graphQLClientKey);
            if (oidcClient == null) {
                oidcClient = defaultOidcClientName;
            }
            Map<String, Uni<String>> dynamicHeaders = configs.getClient(graphQLClientKey).getDynamicHeaders();
            var tokenProviderClass = loadClass(oidcClientToTokenProducerName.get(oidcClient));
            Uni<String> accessTokenProvider = container.<AbstractGraphQLTokenProvider> instance(tokenProviderClass)
                    .get().getAccessToken();
            dynamicHeaders.put("Authorization", accessTokenProvider);
        });
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load generated class " + className, e);
        }
    }
}
