package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that the MariaDB storage engine takes precedence over the MySQL one with a MariaDB database
 */
public class StorageSpecificBothSetMariaDBTest {

    @Inject
    EntityManagerFactory entityManagerFactory;

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    // Sets quarkus.hibernate-orm.dialect.mariadb.storage-engine=innodb
                    .addAsResource("application-start-offline-mariadb-dialect.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-mariadb-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mysql.storage-engine", "myisam");

    @Test
    public void mariaDbStorageEngineIsUsed() {
        Dialect dialect = entityManagerFactory.unwrap(SessionFactoryImpl.class).getJdbcServices().getDialect();
        assertThat(dialect.getTableCreationSupport().tableCreationOptions().toLowerCase(Locale.ROOT))
                .contains("innodb");
    }
}
