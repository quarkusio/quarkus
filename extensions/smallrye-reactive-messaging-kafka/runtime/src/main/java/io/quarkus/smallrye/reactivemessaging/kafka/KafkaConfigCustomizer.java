package io.quarkus.smallrye.reactivemessaging.kafka;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;

import io.quarkus.kafka.client.tls.QuarkusKafkaSslEngineFactory;
import io.smallrye.reactive.messaging.ClientCustomizer;

@ApplicationScoped
public class KafkaConfigCustomizer implements ClientCustomizer<Map<String, Object>> {

    @Override
    public Map<String, Object> customize(String channel, Config channelConfig, Map<String, Object> config) {
        // TODO verify other ssl properties
        if (config.containsKey("tls-configuration-name")) {
            QuarkusKafkaSslEngineFactory.checkForOtherSslConfigs(config);
            config.put("ssl.engine.factory.class", QuarkusKafkaSslEngineFactory.class.getName());
        }
        return config;
    }
}
