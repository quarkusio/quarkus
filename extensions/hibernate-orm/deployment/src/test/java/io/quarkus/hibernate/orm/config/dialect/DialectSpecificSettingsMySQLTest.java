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
 * Test MySQL Dialect specific settings with MySQL DBKind
 */
public class DialectSpecificSettingsMySQLTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mysql-dialect.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-mysql-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mysql.no-backslash-escapes", "true")
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mysql.bytes-per-character", "8");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.mysql.bytes_per_character"))
                .isEqualTo("8");
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.mysql.no_backslash_escapes"))
                .isEqualTo("true");
    }
}
