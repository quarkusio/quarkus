package io.quarkus.kafka.streams.runtime;

import java.util.Properties;

public class KafkaStreamsSupport {

    private final Properties properties;

    public KafkaStreamsSupport(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }
}
