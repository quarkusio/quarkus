package io.quarkus.smallrye.reactivemessaging.mqtt.runtime;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.reactive.messaging.ClientCustomizer;
import io.smallrye.reactive.messaging.mqtt.session.MqttClientSessionOptions;

@ApplicationScoped
public class MqttClientConfigCustomizer implements ClientCustomizer<MqttClientSessionOptions> {

    private static final Logger log = Logger.getLogger(MqttClientConfigCustomizer.class);

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Override
    public MqttClientSessionOptions customize(String channel, Config channelConfig, MqttClientSessionOptions options) {
        Optional<String> tlsConfigName = channelConfig.getOptionalValue("tls-configuration-name", String.class);
        if (tlsConfigName.isPresent()) {
            String tlsConfig = tlsConfigName.get();
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(tlsConfig);
            if (maybeTlsConfig.isPresent()) {
                TlsConfigUtils.configure(options, maybeTlsConfig.get());
                log.debugf("Configured MqttClientSessionOptions for channel %s with TLS configuration %s", channel, tlsConfig);
            }
        }
        return options;
    }
}
