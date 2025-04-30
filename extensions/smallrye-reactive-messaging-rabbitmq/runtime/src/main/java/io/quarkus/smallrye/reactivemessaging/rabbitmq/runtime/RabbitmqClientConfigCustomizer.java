package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.reactive.messaging.ClientCustomizer;
import io.vertx.rabbitmq.RabbitMQOptions;

@ApplicationScoped
public class RabbitmqClientConfigCustomizer implements ClientCustomizer<RabbitMQOptions> {

    private static final Logger log = Logger.getLogger(RabbitmqClientConfigCustomizer.class);

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Override
    public RabbitMQOptions customize(String channel, Config channelConfig, RabbitMQOptions options) {
        Optional<String> tlsConfigName = channelConfig.getOptionalValue("tls-configuration-name", String.class);
        if (tlsConfigName.isPresent()) {
            String tlsConfig = tlsConfigName.get();
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(tlsConfig);
            if (maybeTlsConfig.isPresent()) {
                TlsConfigUtils.configure(options, maybeTlsConfig.get());
                log.debugf("Configured RabbitMQOptions for channel %s with TLS configuration %s", channel, tlsConfig);
            }
        }
        return options;
    }
}
