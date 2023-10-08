package io.quarkus.it.compose.devservices.kafka;

import io.quarkus.test.junit.QuarkusTestProfile;

public class KafkaTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "kafka";
    }
}
