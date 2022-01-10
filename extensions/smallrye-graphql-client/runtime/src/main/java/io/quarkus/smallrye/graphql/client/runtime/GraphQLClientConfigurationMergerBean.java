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

    GraphQLClientsConfiguration upstreamConfiguration;

    @Inject
    GraphQLClientsConfig quarkusConfiguration;

    @Inject
    GraphQLClientSupport support;

    @PostConstruct
    void enhanceGraphQLConfiguration() {
        upstreamConfiguration = GraphQLClientsConfiguration.getInstance();
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
            if (upstreamConfiguration.getClient(configKey) == null) {
                GraphQLClientConfiguration transformed = new GraphQLClientConfiguration();
                transformed.setHeaders(quarkusConfig.headers);
                quarkusConfig.url.ifPresent(transformed::setUrl);
                transformed.setWebsocketSubprotocols(quarkusConfig.subprotocols.orElse(new ArrayList<>()));
                upstreamConfiguration.addClient(configKey, transformed);
            } else {
                // if SmallRye configuration already contains this client, override it with the Quarkus configuration
                GraphQLClientConfiguration upstreamConfig = upstreamConfiguration.getClient(configKey);
                quarkusConfig.url.ifPresent(upstreamConfig::setUrl);
                // merge the headers
                if (quarkusConfig.headers != null) {
                    upstreamConfig.getHeaders().putAll(quarkusConfig.headers);
                }
                if (quarkusConfig.subprotocols.isPresent()) {
                    if (upstreamConfig.getWebsocketSubprotocols() != null) {
                        upstreamConfig.getWebsocketSubprotocols().addAll(quarkusConfig.subprotocols.get());
                    } else {
                        upstreamConfig.setWebsocketSubprotocols(quarkusConfig.subprotocols.get());
                    }
                }
            }
        }

    }

    public void nothing() {
    }
}
