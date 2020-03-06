package io.quarkus.it.mongodb;

import java.util.Collections;
import java.util.Map;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {

    private static final GenericContainer<?> MONGO = new FixedHostPortGenericContainer<>("mongo:4.0")
            .withFixedExposedPort(27018, 27017);

    @Override
    public Map<String, String> start() {
        MONGO.start();
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (MONGO != null) {
            MONGO.stop();
        }
    }
}
