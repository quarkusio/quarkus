package io.quarkus.kafka.client.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KafkaRecorder {

    public void checkBoostrapServers() {
        Config config = ConfigProvider.getConfig();
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
