package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;

import javax.sql.DataSource;

public interface FlywayContainerProducer {
    FlywayContainer createFlyway(DataSource dataSource, String dataSourceName, String name, String id, boolean hasMigrations,
            boolean createPossible);

    Annotation getFlywayContainerQualifier(String name);
}
