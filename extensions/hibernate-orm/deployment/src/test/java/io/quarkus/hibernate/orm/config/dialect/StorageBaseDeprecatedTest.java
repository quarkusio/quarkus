package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test dialects specifics values can be configured
 */
public class StorageBaseDeprecatedTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mysql-dialect.properties", "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-mysql-deployment", Version.getVersion())))
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains("The storage engine set through configuration property "));
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains("is deprecated;"));
            });

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory).isNotNull();
    }
}
