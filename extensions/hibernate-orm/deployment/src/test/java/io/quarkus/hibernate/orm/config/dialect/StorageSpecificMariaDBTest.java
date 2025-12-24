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
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test Maria DB storage engine with MySql dialect
 */
public class StorageSpecificMariaDBTest {

    @Inject
    EntityManagerFactory entityManagerFactory;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mysql-dialect.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-mysql-deployment", Version.getVersion())))
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .overrideConfigKey("quarkus.hibernate-orm.dialect.storage-engine", "")
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mariadb.storage-engine", "innodb");

    @Test
    public void applicationStarts() {
        Dialect dialect = entityManagerFactory.unwrap(SessionFactoryImpl.class).getJdbcServices().getDialect();
        assertThat(dialect.getTableTypeString().toLowerCase(Locale.ROOT)).contains("innodb");
    }
}
