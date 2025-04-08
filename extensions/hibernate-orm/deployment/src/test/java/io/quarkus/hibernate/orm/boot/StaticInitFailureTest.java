package io.quarkus.hibernate.orm.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.SQLDeleteAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that a failure during static init is correctly propagated and doesn't trigger other, cascading failures.
 */
public class StaticInitFailureTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(EntityWithIncorrectMapping.class))
            .withConfigurationResource("application.properties")
            // Expect only one error: the one we triggered
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    // Ignore these particular warnings: they are not relevant to this test.
                    && !record.getMessage().contains("must be used to index an application dependency")) // too old Jandex
            // In particular we don't want a log telling us JPAConfig could not be created
            // because HibernateOrmRuntimeConfig is not initialized yet.
            // JPAConfig should not be created in the first place!
            // See https://github.com/quarkusio/quarkus/issues/32188#issuecomment-1488037517
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage).isEmpty())
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessageContaining("@SQLDeleteAll")
                    .hasNoSuppressedExceptions());

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

    @Entity(name = "myentity")
    @SQLDeleteAll(sql = "foo") // Triggers a failure because this annotation shouldn't be applied to entities
    public static class EntityWithIncorrectMapping {
        private long id;

        public EntityWithIncorrectMapping() {
        }

        @Id
        @GeneratedValue
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }
    }

}
