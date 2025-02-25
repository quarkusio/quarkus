package io.quarkus.smallrye.graphql.client.runtime;

import static io.quarkus.tls.runtime.config.TlsConfig.DEFAULT_NAME;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;

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
                .setSslOptions(updatedTlsConfiguration.getSSLOptions()); // CLR
    }
}
