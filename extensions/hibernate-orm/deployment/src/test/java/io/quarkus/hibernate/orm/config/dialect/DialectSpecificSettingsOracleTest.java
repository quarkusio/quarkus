package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test Oracle Dialect specific settings with Oracle DBKind
 */
public class DialectSpecificSettingsOracleTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-oracle-dialect.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-oracle-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.hibernate-orm.dialect.oracle.application-continuity", "true")
            .overrideConfigKey("quarkus.hibernate-orm.dialect.oracle.autonomous", "true")
            .overrideConfigKey("quarkus.hibernate-orm.dialect.oracle.extended", "true");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.oracle.application_continuity"))
                .isEqualTo("true");
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.oracle.is_autonomous"))
                .isEqualTo("true");
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.oracle.extended_string_size"))
                .isEqualTo("true");
    }
}
