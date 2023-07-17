package io.quarkus.smallrye.graphql.client.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
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

    private final Logger logger = Logger.getLogger(GraphQLClientConfigurationMergerBean.class);

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

        // allow automatically wiring client to the local server instance in test mode
        if (LaunchMode.current() == LaunchMode.TEST) {
            String testUrl = null;
            for (String configKey : support.getKnownConfigKeys()) {
                GraphQLClientConfiguration config = upstreamConfigs.getClient(configKey);
                if (config.getUrl() == null) {
                    if (testUrl == null) {
                        testUrl = getTestingServerUrl();
                    }
                    logger.info("Automatically wiring the URL of GraphQL client named " + configKey + " to " + testUrl
                            + ". If this is incorrect, " +
                            "please set it manually using the quarkus.smallrye-graphql-client." + maybeWithQuotes(configKey)
                            + ".url property. Also note that" +
                            " this autowiring is only supported during tests.");
                    config.setUrl(testUrl);
                }
            }
        }
    }

    private String maybeWithQuotes(String key) {
        if (key.contains(".")) {
            return "\"" + key + "\"";
        } else {
            return key;
        }
    }

    // translates a Quarkus `GraphQLClientConfig` configuration object to `GraphQLClientConfiguration` which is understood
    // by SmallRye GraphQL
    private GraphQLClientConfiguration toSmallRyeNativeConfiguration(GraphQLClientConfig quarkusConfig) {
        GraphQLClientConfiguration transformed = new GraphQLClientConfiguration();
        transformed.setHeaders(quarkusConfig.headers);
        transformed.setInitPayload(Optional.ofNullable(quarkusConfig.initPayload)
                .map(m -> new HashMap<String, Object>(m)).orElse(null));
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
        quarkusConfig.allowUnexpectedResponseFields.ifPresent(transformed::setAllowUnexpectedResponseFields);
        return transformed;
    }

    private String getTestingServerUrl() {
        Config config = ConfigProvider.getConfig();
        // the client extension doesn't have dependencies on neither the server extension nor quarkus-vertx-http, so guessing
        // is somewhat limited
        return "http://localhost:" + config.getOptionalValue("quarkus.http.test-port", int.class).orElse(8081) + "/graphql";
    }

    public void nothing() {
    }
}
