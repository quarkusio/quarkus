package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime;

import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.SslContextFactory;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.reactive.messaging.ClientCustomizer;

@ApplicationScoped
public class RabbitmqClientConfigCustomizer implements ClientCustomizer<ConnectionFactory> {

    private static final Logger log = Logger.getLogger(RabbitmqClientConfigCustomizer.class);

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Override
    public ConnectionFactory customize(String channel, Config channelConfig, ConnectionFactory connectionFactory) {
        Optional<String> tlsConfigName = channelConfig.getOptionalValue("tls-configuration-name", String.class);
        if (tlsConfigName.isPresent()) {
            String tlsConfig = tlsConfigName.get();
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(tlsConfig);
            if (maybeTlsConfig.isPresent()) {
                TlsConfiguration tlsConfiguration = maybeTlsConfig.get();
                connectionFactory.setSslContextFactory(new SslContextFactory() {
                    @Override
                    public SSLContext create(String name) {
                        try {
                            return tlsConfiguration.createSSLContext();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }
                });
                log.debugf("Configured RabbitMQOptions for channel %s with TLS configuration %s", channel, tlsConfig);
            }
        }
        return connectionFactory;
    }
}
