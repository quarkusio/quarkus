package io.quarkus.it.opentelemetry;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class Db2LifecycleManager implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(Db2LifecycleManager.class);
    private static final String QUARKUS = "quarkus";
    private static final String DB2_IMAGE = System.getProperty("db2.image");
    private StartedDb2Container db2Container;

    @Override
    public Map<String, String> start() {
        db2Container = new StartedDb2Container();
        LOGGER.info(db2Container.getLogs());

        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.datasource.db2.jdbc.url",
                String.format("jdbc:db2://%s:%s/%s", db2Container.getHost(),
                        db2Container.getFirstMappedPort(), QUARKUS));
        properties.put("quarkus.datasource.db2.password", QUARKUS);
        properties.put("quarkus.datasource.db2.username", QUARKUS);
        properties.put("quarkus.hibernate-orm.db2.schema-management.strategy", "drop-and-create");
        properties.put("quarkus.hibernate-orm.db2.active", "true");
        properties.put("quarkus.hibernate-orm.oracle.active", "false");
        properties.put("quarkus.hibernate-orm.postgresql.active", "false");
        properties.put("quarkus.hibernate-orm.mariadb.active", "false");
        properties.put("quarkus.hibernate-orm.h2.active", "false");

        return properties;
    }

    @Override
    public void stop() {
        db2Container.stop();
    }

    private static final class StartedDb2Container extends Db2Container {

        public StartedDb2Container() {
            super(DockerImageName
                    .parse(DB2_IMAGE)
                    .asCompatibleSubstituteFor(DockerImageName.parse("ibmcom/db2")));
            withDatabaseName(QUARKUS);
            withUsername(QUARKUS);
            withPassword(QUARKUS);
            addExposedPort(5000);
            acceptLicense();
            start();
        }
    }
}
