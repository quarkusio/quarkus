package io.quarkus.smallrye.graphql.client.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.graphql.client.model.ClientModels;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import io.smallrye.graphql.client.vertx.VertxManager;
import io.smallrye.graphql.client.vertx.typesafe.VertxTypesafeGraphQLClientBuilder;
import io.vertx.core.Vertx;

@Recorder
public class SmallRyeGraphQLClientRecorder {
    private final Logger logger = Logger.getLogger(SmallRyeGraphQLClientRecorder.class);

    private final RuntimeValue<GraphQLClientsConfig> runtimeConfig;

    public SmallRyeGraphQLClientRecorder(final RuntimeValue<GraphQLClientsConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public <T> Function<SyntheticCreationalContext<T>, T> typesafeClientSupplier(Class<T> targetClassName) {
        return new Function<>() {
            @Override
            public T apply(SyntheticCreationalContext<T> context) {
                TypesafeGraphQLClientBuilder builder = TypesafeGraphQLClientBuilder.newBuilder();
                ClientModels clientModels = context.getInjectedReference(ClientModels.class);
                return (((VertxTypesafeGraphQLClientBuilder) builder).clientModels(clientModels)).build(targetClassName);
            }
        };
    }

    public void setTypesafeApiClasses(List<String> apiClassNames) {
        GraphQLClientsConfiguration.setSingleApplication(true);
        GraphQLClientsConfiguration configBean = GraphQLClientsConfiguration.getInstance();
        List<Class<?>> classes = apiClassNames.stream().map(className -> {
            try {
                return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        configBean.addTypesafeClientApis(classes);
    }

    public void mergeClientConfigurations(GraphQLClientSupport support) {
        GraphQLClientsConfiguration upstreamConfigs = GraphQLClientsConfiguration.getInstance();
        for (Map.Entry<String, GraphQLClientConfig> client : runtimeConfig.getValue().clients().entrySet()) {
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

    public void setGlobalVertxInstance(Supplier<Vertx> vertx) {
        VertxManager.setFromGlobal(vertx.get());
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
        transformed.setHeaders(quarkusConfig.headers());
        transformed.setInitPayload(Optional.ofNullable(quarkusConfig.initPayload())
                .map(m -> new HashMap<String, Object>(m)).orElse(null));
        quarkusConfig.url().ifPresent(transformed::setUrl);
        transformed.setWebsocketSubprotocols(quarkusConfig.subprotocols().orElse(new ArrayList<>()));
        resolveTlsConfigurationForRegistry(quarkusConfig)
                .ifPresentOrElse(tlsConfiguration -> {
                    transformed.setTlsKeyStoreOptions(tlsConfiguration.getKeyStoreOptions());
                    transformed.setTlsTrustStoreOptions(tlsConfiguration.getTrustStoreOptions());
                    transformed.setSslOptions(tlsConfiguration.getSSLOptions());
                    tlsConfiguration.getHostnameVerificationAlgorithm()
                            .ifPresent(transformed::setHostnameVerificationAlgorithm);
                    transformed.setUsesSni(Boolean.valueOf(tlsConfiguration.usesSni()));
                }, () -> {
                    // DEPRECATED
                    quarkusConfig.keyStore().ifPresent(transformed::setKeyStore);
                    quarkusConfig.keyStoreType().ifPresent(transformed::setKeyStoreType);
                    quarkusConfig.keyStorePassword().ifPresent(transformed::setKeyStorePassword);
                    quarkusConfig.trustStore().ifPresent(transformed::setTrustStore);
                    quarkusConfig.trustStoreType().ifPresent(transformed::setTrustStoreType);
                    quarkusConfig.trustStorePassword().ifPresent(transformed::setTrustStorePassword);
                });
        quarkusConfig.proxyHost().ifPresent(transformed::setProxyHost);
        quarkusConfig.proxyPort().ifPresent(transformed::setProxyPort);
        quarkusConfig.proxyUsername().ifPresent(transformed::setProxyUsername);
        quarkusConfig.proxyPassword().ifPresent(transformed::setProxyPassword);
        quarkusConfig.maxRedirects().ifPresent(transformed::setMaxRedirects);
        quarkusConfig.executeSingleResultOperationsOverWebsocket()
                .ifPresent(transformed::setExecuteSingleOperationsOverWebsocket);
        quarkusConfig.websocketInitializationTimeout().ifPresent(transformed::setWebsocketInitializationTimeout);
        quarkusConfig.allowUnexpectedResponseFields().ifPresent(transformed::setAllowUnexpectedResponseFields);
        transformed.setDynamicHeaders(new HashMap<>());
        return transformed;
    }

    private String getTestingServerUrl() {
        Config config = ConfigProvider.getConfig();
        // the client extension doesn't have dependencies on neither the server extension nor quarkus-vertx-http, so guessing
        // is somewhat limited
        return "http://localhost:" + config.getOptionalValue("quarkus.http.test-port", int.class).orElse(8081) + "/graphql";
    }

    public RuntimeValue<ClientModels> getRuntimeClientModel(ClientModels clientModel) {
        return new RuntimeValue<>(clientModel);
    }

    private Optional<TlsConfiguration> resolveTlsConfigurationForRegistry(GraphQLClientConfig quarkusConfig) {
        if (Arc.container() != null) {
            TlsConfigurationRegistry tlsConfigurationRegistry = Arc.container().select(TlsConfigurationRegistry.class).orNull();
            if (tlsConfigurationRegistry != null) {
                if (tlsConfigurationRegistry.getDefault().isPresent()
                        && (tlsConfigurationRegistry.getDefault().get().getTrustStoreOptions() != null
                                || tlsConfigurationRegistry.getDefault().get().isTrustAll())) {
                    return tlsConfigurationRegistry.getDefault();
                }
                return TlsConfiguration.from(tlsConfigurationRegistry, quarkusConfig.tlsConfigurationName());
            }
        }
        return Optional.empty();
    }
}
