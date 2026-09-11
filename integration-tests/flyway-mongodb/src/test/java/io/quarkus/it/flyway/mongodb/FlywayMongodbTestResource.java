package io.quarkus.it.flyway.mongodb;

import java.util.Map;

import org.testcontainers.mongodb.MongoDBContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class FlywayMongodbTestResource implements QuarkusTestResourceLifecycleManager {

    private MongoDBContainer container;

    @Override
    public Map<String, String> start() {
        container = new MongoDBContainer("mongo:7.0");
        container.start();
        String base = container.getConnectionString();
        return Map.of(
                "quarkus.mongodb.connection-string", base + "/testdb",
                "quarkus.mongodb.secondary.connection-string", base + "/secondarydb",
                "quarkus.mongodb.users.connection-string", base + "/usersdb",
                "quarkus.mongodb.lazy.connection-string", base + "/lazydb",
                "quarkus.mongodb.custom-ph.connection-string", base + "/customphdb");
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
