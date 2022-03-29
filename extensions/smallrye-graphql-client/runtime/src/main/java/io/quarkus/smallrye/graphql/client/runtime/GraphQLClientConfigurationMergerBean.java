package io.quarkus.smallrye.graphql.client.runtime;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;

/**
 * On startup, this beans takes Quarkus-specific configuration of GraphQL clients (quarkus.* properties)
 * and merges this configuration with the configuration parsed by SmallRye GraphQL itself (CLIENT/mp-graphql/* properties)
 *
 * The resulting merged configuration resides in `io.smallrye.graphql.client.GraphQLClientsConfiguration`
 *
 * Quarkus configuration overrides SmallRye configuration where applicable.
 */
@Singleton
public class GraphQLClientConfigurationMergerBean {

    GraphQLClientsConfiguration upstreamConfigs;

    @Inject
    GraphQLClientsConfig quarkusConfiguration;

    @Inject
    GraphQLClientSupport support;

    @PostConstruct
    void enhanceGraphQLConfiguration() {
        upstreamConfigs = GraphQLClientsConfiguration.getInstance();
        for (Map.Entry<String, GraphQLClientConfig> client : quarkusConfiguration.clients.entrySet()) {
            // the raw config key provided in the config, this might be a short class name,
            // so translate that into the fully qualified name if applicable
            String rawConfigKey = client.getKey();
            Map<String, String> shortNamesToQualifiedNamesMapping = support.getShortNamesToQualifiedNamesMapping();
            String configKey = shortNamesToQualifiedNamesMapping != null &&
                    shortNamesToQualifiedNamesMapping.containsKey(rawConfigKey)
                            ? shortNamesToQualifiedNamesMapping.get(rawConfigKey)
                            : rawConfigKey;

            GraphQLClientConfig quarkusConfig = client.getValue();
            // if SmallRye configuration does not contain this client, simply use it
            GraphQLClientConfiguration upstreamConfig = upstreamConfigs.getClient(configKey);
            if (upstreamConfig == null) {
                GraphQLClientConfiguration transformed = toSmallRyeNativeConfiguration(quarkusConfig);
                upstreamConfigs.addClient(configKey, transformed);
            } else {
                // if SmallRye configuration already contains this client, enhance it with the Quarkus configuration
                upstreamConfig.merge(toSmallRyeNativeConfiguration(quarkusConfig));
            }
        }

    }

    // translates a Quarkus `GraphQLClientConfig` configuration object to `GraphQLClientConfiguration` which is understood
    // by SmallRye GraphQL
    private GraphQLClientConfiguration toSmallRyeNativeConfiguration(GraphQLClientConfig quarkusConfig) {
        GraphQLClientConfiguration transformed = new GraphQLClientConfiguration();
        transformed.setHeaders(quarkusConfig.headers);
        quarkusConfig.url.ifPresent(transformed::setUrl);
        transformed.setWebsocketSubprotocols(quarkusConfig.subprotocols.orElse(new ArrayList<>()));
        quarkusConfig.keyStore.ifPresent(transformed::setKeyStore);
        quarkusConfig.keyStoreType.ifPresent(transformed::setKeyStoreType);
        quarkusConfig.keyStorePassword.ifPresent(transformed::setKeyStorePassword);
        quarkusConfig.trustStore.ifPresent(transformed::setTrustStore);
        quarkusConfig.trustStoreType.ifPresent(transformed::setTrustStoreType);
        quarkusConfig.trustStorePassword.ifPresent(transformed::setTrustStorePassword);
        quarkusConfig.proxyHost.ifPresent(transformed::setProxyHost);
        quarkusConfig.proxyPort.ifPresent(transformed::setProxyPort);
        quarkusConfig.proxyUsername.ifPresent(transformed::setProxyUsername);
        quarkusConfig.proxyPassword.ifPresent(transformed::setProxyPassword);
        quarkusConfig.maxRedirects.ifPresent(transformed::setMaxRedirects);
        quarkusConfig.executeSingleResultOperationsOverWebsocket
                .ifPresent(transformed::setExecuteSingleOperationsOverWebsocket);
        quarkusConfig.websocketInitializationTimeout.ifPresent(transformed::setWebsocketInitializationTimeout);
        return transformed;
    }

    public void nothing() {
    }
}
