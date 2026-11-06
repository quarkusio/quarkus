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
 * Test that an application using DB2 can start offline.
 * <p>
 * DB2 does not support local temporary tables, so Quarkus must not force the local temporary table strategies.
 */
public class StartOfflineDb2Test {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-db2-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.datasource.db-kind", "db2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:db2://localhost:50000/test")
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory).isNotNull();
    }

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
