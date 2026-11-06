package io.quarkus.kafka.client.runtime;

import java.util.Optional;

import io.smallrye.config.Config;

public class KafkaRecorder {

    public static void checkBoostrapServers() {
        Config config = Config.get();
        Boolean serviceBindingEnabled = config.getValue("quarkus.kubernetes-service-binding.enabled", Boolean.class);
        if (!serviceBindingEnabled) {
            return;
        }
        Optional<String> boostrapServersOptional = config.getOptionalValue("kafka.bootstrap.servers", String.class);
        if (boostrapServersOptional.isEmpty()) {
            throw new IllegalStateException(
                    "The property 'kafka.bootstrap.servers' must be set when 'quarkus.kubernetes-service-binding.enabled' has been set to 'true'");
        }
    }
}
