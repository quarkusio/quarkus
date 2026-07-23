package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that when db-version-defaults=recent (the default), the default version is applied from Agroal's defaults.
 */
public class DbVersionDefaultsRecentTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            // Add postgresql, because H2's versioning is fixed
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            // Explicitly select "recent" defaults (which is... the default)
            .overrideConfigKey("quarkus.datasource.db-version-defaults", "recent")
            // Starting offline for faster testing -- shouldn't affect test results
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void testApplicationStarts() {
        String expectedVersion = DatabaseVersionLoader.loadDefaultVersion("postgresql");

        var dialect = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        String actualVersion = DialectVersions.toString(dialect.getVersion());
        assertThat(actualVersion)
                .startsWith(expectedVersion);
    }
}
