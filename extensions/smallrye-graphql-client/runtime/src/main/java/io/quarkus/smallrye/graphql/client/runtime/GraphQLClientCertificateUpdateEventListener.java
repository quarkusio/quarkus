package io.quarkus.smallrye.graphql.client.runtime;

import static io.quarkus.tls.runtime.config.TlsConfig.DEFAULT_NAME;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.graphql.client.impl.dynamic.cdi.NamedDynamicClients;
import io.smallrye.graphql.client.vertx.dynamic.VertxDynamicGraphQLClient;
import io.vertx.core.http.HttpClient;

@Singleton
public class GraphQLClientCertificateUpdateEventListener {

    private final static Logger LOG = Logger.getLogger(GraphQLClientCertificateUpdateEventListener.class);

    @Inject
    GraphQLClientsConfig graphQLClientsConfig;

    public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
        String updatedTlsConfigurationName = event.name();
        TlsConfiguration updatedTlsConfiguration = event.tlsConfiguration();
        graphQLClientsConfig.clients()
                .forEach((configKey, clientConfig) -> {
                    GraphQLClientConfiguration graphQLClientConfiguration = GraphQLClientsConfiguration.getInstance()
                            .getClient(configKey);
                    clientConfig.tlsConfigurationName().ifPresentOrElse(tlsConfigurationName -> {
                        if (tlsConfigurationName.equals(updatedTlsConfigurationName)) {
                            updateConfiguration(updatedTlsConfigurationName, updatedTlsConfiguration,
                                    graphQLClientConfiguration, configKey);
                        }
                    }, () -> {
                        if (DEFAULT_NAME.equals(updatedTlsConfigurationName)) {
                            updateConfiguration("default", updatedTlsConfiguration, graphQLClientConfiguration, configKey);
                        }
                    });
                });

        updateLiveHttpClients(updatedTlsConfigurationName, updatedTlsConfiguration);
    }

    private void updateLiveHttpClients(String tlsConfigName, TlsConfiguration updatedTlsConfiguration) {
        // Update typesafe clients registered via the recorder
        List<HttpClient> httpClients = SmallRyeGraphQLClientRecorder.clientsUsingTlsConfig(tlsConfigName);
        for (HttpClient httpClient : httpClients) {
            doUpdateSSLOptions(httpClient, tlsConfigName, updatedTlsConfiguration);
        }

        // Update dynamic clients created via NamedDynamicClients
        updateDynamicClients(tlsConfigName, updatedTlsConfiguration);
    }

    private void updateDynamicClients(String tlsConfigName, TlsConfiguration updatedTlsConfiguration) {
        NamedDynamicClients namedDynamicClients = Arc.container().instance(NamedDynamicClients.class).get();
        if (namedDynamicClients == null) {
            return;
        }
        Map<String, DynamicGraphQLClient> createdClients = namedDynamicClients.getCreatedClients();
        if (createdClients == null || createdClients.isEmpty()) {
            return;
        }
        graphQLClientsConfig.clients().forEach((configKey, clientConfig) -> {
            boolean matches = clientConfig.tlsConfigurationName()
                    .map(name -> name.equals(tlsConfigName))
                    .orElse(DEFAULT_NAME.equals(tlsConfigName));
            if (matches) {
                DynamicGraphQLClient client = createdClients.get(configKey);
                if (client instanceof VertxDynamicGraphQLClient) {
                    HttpClient httpClient = ((VertxDynamicGraphQLClient) client).getHttpClient();
                    if (httpClient != null) {
                        doUpdateSSLOptions(httpClient, tlsConfigName, updatedTlsConfiguration);
                    }
                }
            }
        });
    }

    private void doUpdateSSLOptions(HttpClient httpClient, String tlsConfigName,
            TlsConfiguration updatedTlsConfiguration) {
        httpClient.updateSSLOptions(updatedTlsConfiguration.getSSLOptions()).onComplete(result -> {
            if (result.succeeded()) {
                LOG.infof(
                        "SSL options updated on live HttpClient for GraphQL client(s) using TLS configuration '%s'",
                        tlsConfigName);
            } else {
                LOG.errorf(result.cause(),
                        "Failed to update SSL options on live HttpClient for GraphQL client(s) using TLS configuration '%s'",
                        tlsConfigName);
            }
        });
    }

    private void updateConfiguration(String tlsBucketName, TlsConfiguration updatedTlsConfiguration,
            GraphQLClientConfiguration graphQLClientConfiguration, String configKey) {
        LOG.infof("Certificate reloaded for the client '%s' using the TLS configuration (bucket) name '%s'", configKey,
                tlsBucketName);
        graphQLClientConfiguration
                .setTlsKeyStoreOptions(updatedTlsConfiguration.getKeyStoreOptions());
        graphQLClientConfiguration
                .setTlsTrustStoreOptions(updatedTlsConfiguration.getTrustStoreOptions());
        graphQLClientConfiguration
                .setSslOptions(updatedTlsConfiguration.getClientSSLOptions());
    }
}
