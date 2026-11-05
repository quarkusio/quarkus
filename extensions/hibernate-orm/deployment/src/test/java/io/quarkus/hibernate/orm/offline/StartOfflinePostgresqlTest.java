package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.QuerySettings;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that an application using PostgreSQL and starting offline keeps the dialect's default
 * multi-table mutation strategies, since PostgreSQL supports CTE.
 */
public class StartOfflinePostgresqlTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:5432/test")
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void localTemporaryTableStrategiesAreNotForced() {
        var settings = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .getService(ConfigurationServiceImpl.class)
                .getSettings();
        assertThat(settings.get(QuerySettings.QUERY_MULTI_TABLE_INSERT_STRATEGY)).isNull();
        assertThat(settings.get(QuerySettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY)).isNull();
    }
}
