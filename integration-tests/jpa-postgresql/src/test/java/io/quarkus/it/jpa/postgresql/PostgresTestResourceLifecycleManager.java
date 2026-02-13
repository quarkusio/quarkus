package io.quarkus.it.jpa.postgresql;

import java.util.Map;

import org.testcontainers.containers.PostgreSQLContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PostgresTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static PostgreSQLContainer<?> postgres;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>("postgres:18") // the exact value doesn't really matter here
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");

        postgres.start();

        return Map.of("quarkus.datasource.jdbc.url", postgres.getJdbcUrl(), "quarkus.datasource.username", "test",
                "quarkus.datasource.password", "test");
    }

    @Override
    public void stop() {

        if (postgres != null) {
            postgres.stop();
        }
    }

}
