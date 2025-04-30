package io.quarkus.it.opentelemetry;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MariaDbLifecycleManager implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(MariaDbLifecycleManager.class);
    private static final String QUARKUS = "quarkus";
    private static final String MARIADB_IMAGE = System.getProperty("mariadb.image");
    private StartedMariaDBContainer mariaDbContainer;

    @Override
    public Map<String, String> start() {
        mariaDbContainer = new StartedMariaDBContainer();
        LOGGER.info(mariaDbContainer.getLogs());

        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.datasource.mariadb.jdbc.url",
                String.format("jdbc:mariadb://%s:%s/%s", mariaDbContainer.getHost(),
                        mariaDbContainer.getFirstMappedPort(), QUARKUS));
        properties.put("quarkus.datasource.mariadb.password", QUARKUS);
        properties.put("quarkus.datasource.mariadb.username", QUARKUS);
        properties.put("quarkus.hibernate-orm.mariadb.schema-management.strategy", "drop-and-create");
        properties.put("quarkus.hibernate-orm.mariadb.active", "true");
        properties.put("quarkus.hibernate-orm.oracle.active", "false");
        properties.put("quarkus.hibernate-orm.postgresql.active", "false");
        properties.put("quarkus.hibernate-orm.db2.active", "false");
        properties.put("quarkus.hibernate-orm.h2.active", "false");

        return properties;
    }

    @Override
    public void stop() {
        mariaDbContainer.stop();
    }

    private static final class StartedMariaDBContainer extends MariaDBContainer<StartedMariaDBContainer> {

        public StartedMariaDBContainer() {
            super(DockerImageName
                    .parse(MARIADB_IMAGE)
                    .asCompatibleSubstituteFor(DockerImageName.parse(MariaDBContainer.NAME)));
            withDatabaseName(QUARKUS);
            withUsername(QUARKUS);
            withPassword(QUARKUS);
            addExposedPort(3306);
            start();
        }
    }
}
