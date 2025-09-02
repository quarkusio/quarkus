package io.quarkus.it.opentelemetry;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PostgreSqlLifecycleManager implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(PostgreSqlLifecycleManager.class);
    private static final String QUARKUS = "quarkus";
    private static final String POSTGRES_IMAGE = System.getProperty("postgres.image");
    private StartedPostgresContainer postgresContainer;

    @Override
    public Map<String, String> start() {
        postgresContainer = new StartedPostgresContainer();
        LOGGER.info(postgresContainer.getLogs());

        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.datasource.postgresql.jdbc.url",
                String.format("jdbc:postgresql://%s:%s/%s", postgresContainer.getHost(),
                        postgresContainer.getFirstMappedPort(), QUARKUS));
        properties.put("quarkus.datasource.postgresql.password", QUARKUS);
        properties.put("quarkus.datasource.postgresql.username", QUARKUS);
        properties.put("quarkus.hibernate-orm.postgresql.schema-management.strategy", "drop-and-create");
        properties.put("quarkus.hibernate-orm.postgresql.active", "true");
        properties.put("quarkus.hibernate-orm.oracle.active", "false");
        properties.put("quarkus.hibernate-orm.mariadb.active", "false");
        properties.put("quarkus.hibernate-orm.db2.active", "false");
        properties.put("quarkus.hibernate-orm.h2.active", "false");

        return properties;
    }

    @Override
    public void stop() {
        postgresContainer.stop();
    }

    private static final class StartedPostgresContainer extends PostgreSQLContainer<StartedPostgresContainer> {

        public StartedPostgresContainer() {
            super(DockerImageName
                    .parse(POSTGRES_IMAGE)
                    .asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE)));
            withDatabaseName(QUARKUS);
            withUsername(QUARKUS);
            withPassword(QUARKUS);
            addExposedPort(5432);
            start();
        }
    }
}
