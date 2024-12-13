package io.quarkus.pulsar;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.impl.auth.AuthenticationTls;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.keystores.ExpiryTrustOptions;
import io.smallrye.reactive.messaging.ClientCustomizer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

@ApplicationScoped
public class PulsarClientConfigCustomizer implements ClientCustomizer<ClientBuilder> {

    private static final Logger log = Logger.getLogger(PulsarClientConfigCustomizer.class);

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Override
    public ClientBuilder customize(String channel, Config channelConfig, ClientBuilder builder) {
        Optional<String> tlsConfigName = channelConfig.getOptionalValue("tls-configuration-name", String.class);
        if (tlsConfigName.isPresent()) {
            String tlsConfig = tlsConfigName.get();
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(tlsConfig);
            if (maybeTlsConfig.isPresent()) {
                TlsConfiguration configuration = maybeTlsConfig.get();
                SSLOptions sslOptions = configuration.getSSLOptions();
                builder.tlsCiphers(sslOptions.getEnabledCipherSuites());
                builder.tlsProtocols(sslOptions.getEnabledSecureTransportProtocols());
                builder.allowTlsInsecureConnection(false);

                KeyCertOptions keyStoreOptions = configuration.getKeyStoreOptions();
                TrustOptions trustStoreOptions = configuration.getTrustStoreOptions();

                if (trustStoreOptions instanceof ExpiryTrustOptions) {
                    trustStoreOptions = ((ExpiryTrustOptions) trustStoreOptions).unwrap();
                }

                if (keyStoreOptions instanceof PemKeyCertOptions keyCertOptions
                        && trustStoreOptions instanceof PemTrustOptions trustCertOptions) {
                    Buffer trust = trustCertOptions.getCertValues().stream()
                            .collect(Buffer::buffer, Buffer::appendBuffer, Buffer::appendBuffer);
                    builder.authentication(new AuthenticationTls(
                            () -> new ByteArrayInputStream(keyCertOptions.getCertValue().getBytes()),
                            () -> new ByteArrayInputStream(keyCertOptions.getKeyValue().getBytes()),
                            () -> new ByteArrayInputStream(trust.getBytes())));
                    log.debugf("Configured PulsarClientConfiguration for channel %s with TLS configuration %s",
                            channel, tlsConfig);
                } else if (keyStoreOptions instanceof KeyStoreOptionsBase
                        && trustStoreOptions instanceof KeyStoreOptionsBase) {
                    // Set to false even though we use keyStore TLS
                    builder.useKeyStoreTls(false);
                    builder.authentication(new QuarkusPulsarKeyStoreAuthentication(configuration));
                    log.debugf("Configured PulsarClientConfiguration for channel %s with TLS configuration %s",
                            channel, tlsConfig);
                } else {
                    log.warnf("Unsupported TLS configuration for channel %s with TLS configuration %s", channel, tlsConfig);
                }
            }
        }
        return builder;
    }

}
