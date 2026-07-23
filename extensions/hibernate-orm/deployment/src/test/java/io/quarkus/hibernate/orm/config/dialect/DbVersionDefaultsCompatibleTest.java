package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that when db-version-defaults=compatible, the default version is applied from Hibernate ORM's defaults,
 * as long as that particular dialect doesn't have its version overridden in Quarkus' Hibernate ORM extension.
 * <p>
 * This is mostly intended for use in Keycloak.
 *
 * @see DbVersionDefaultsCompatibleWithQuarkusOverrideTest
 */
public class DbVersionDefaultsCompatibleTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            // Add postgresql, because H2's version is fixed
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            // Explicitly select "compatible" defaults
            .overrideConfigKey("quarkus.datasource.db-version-defaults", "compatible")
            // Starting offline for faster testing -- shouldn't affect test results
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void testApplicationStarts() {
        String expectedVersion = DialectVersions.toString(new PostgreSQLDialect().getVersion());

        var dialect = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        String actualVersion = DialectVersions.toString(dialect.getVersion());
        assertThat(actualVersion)
                .isEqualTo(expectedVersion);
    }
}
