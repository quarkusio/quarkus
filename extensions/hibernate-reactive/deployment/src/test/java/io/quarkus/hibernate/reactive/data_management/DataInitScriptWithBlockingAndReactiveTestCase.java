package io.quarkus.hibernate.reactive.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * When both a blocking and a reactive session factory are created for the same persistence unit,
 * the data init script must be executed only once, like schema management.
 */
public class DataInitScriptWithBlockingAndReactiveTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("data-init-script-blocking-and-reactive-test.sql", "data.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ))
            // No URLs: Dev Services provide a PostgreSQL database for both the blocking and the reactive datasource
            .overrideConfigKey("quarkus.datasource.reactive", "true")
            .overrideConfigKey("quarkus.datasource.username", "hibernate_orm_test")
            .overrideConfigKey("quarkus.datasource.password", "hibernate_orm_test")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "update")
            .overrideConfigKey("quarkus.hibernate-orm.log.format-sql", "false")
            .overrideConfigKey("quarkus.hibernate-orm.log.highlight-sql", "false")
            .overrideConfigKey("quarkus.log.category.\"org.hibernate.SQL\".level", "DEBUG")
            .setLogRecordPredicate(record -> "org.hibernate.SQL".equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records.stream().map(LogRecord::getMessage))
                    .filteredOn(message -> message.contains("Galadriel"))
                    .as("Data init script statements")
                    .hasSize(1));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void dataInitScriptExecutedOnce(UniAsserter asserter) {
        asserter.assertThat(() -> sessionFactory.withSession(s -> s
                .createQuery("from Hero h where h.name = :name", Hero.class).setParameter("name", "Galadriel")
                .getResultList()),
                list -> assertThat(list).hasSize(1));
    }

    @Entity(name = "Hero")
    @Table(name = Hero.TABLE)
    public static class Hero {

        public static final String TABLE = "Hero_for_BlockingAndReactiveDataInitScriptTest";

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public java.lang.Long id;

        public String name;

    }

}
