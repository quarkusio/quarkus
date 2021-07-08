package io.quarkus.smallrye.graphql.client.runtime;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.smallrye.graphql.client.GraphQLClientConfiguration;
import io.smallrye.graphql.client.GraphQLClientsConfiguration;

/**
 * On startup, this beans takes Quarkus-specific configuration of GraphQL clients (quarkus.* properties)
 * and merges this configuration with the configuration parsed by SmallRye GraphQL itself (CLIENT/mp-graphql/* properties)
 *
 * The resulting merged configuration resides in the application-scoped `io.smallrye.graphql.client.GraphQLClientConfiguration`
 *
 * Quarkus configuration overrides SmallRye configuration where applicable.
 */
@Singleton
public class GraphQLClientConfigurationMergerBean {

    @Inject
    GraphQLClientsConfiguration upstreamConfiguration;

    @Inject
    GraphQLClientsConfig quarkusConfiguration;

    @Inject
    GraphQLClientSupport support;

    @PostConstruct
    void enhanceGraphQLConfiguration() {
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
            if (!upstreamConfiguration.getClients().containsKey(configKey)) {
                GraphQLClientConfiguration transformed = new GraphQLClientConfiguration();
                transformed.setHeaders(quarkusConfig.headers);
                quarkusConfig.url.ifPresent(transformed::setUrl);
                upstreamConfiguration.getClients().put(configKey, transformed);
            } else {
                // if SmallRye configuration already contains this client, override it with the Quarkus configuration
                GraphQLClientConfiguration upstreamConfig = upstreamConfiguration.getClients().get(configKey);
                quarkusConfig.url.ifPresent(upstreamConfig::setUrl);
                // merge the headers
                if (quarkusConfig.headers != null) {
                    upstreamConfig.getHeaders().putAll(quarkusConfig.headers);
                }
            }
        }

    }

    public void nothing() {
    }
}
