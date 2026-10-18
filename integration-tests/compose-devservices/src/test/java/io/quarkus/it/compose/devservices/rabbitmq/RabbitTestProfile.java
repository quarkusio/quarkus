package io.quarkus.it.compose.devservices.rabbitmq;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RabbitTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "rabbit";
    }
}
