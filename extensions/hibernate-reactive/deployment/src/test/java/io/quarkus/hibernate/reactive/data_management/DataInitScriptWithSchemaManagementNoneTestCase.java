package io.quarkus.hibernate.reactive.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * With the "none" schema management strategy, e.g. when the schema is managed by Flyway or Liquibase,
 * the data init script (data.sql) is still executed on startup.
 * <p>
 * The script creates the table itself, standing in for the tool managing the schema.
 */
public class DataInitScriptWithSchemaManagementNoneTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("data-init-script-schema-management-none-test.sql", "data.sql"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void dataInitScriptExecuted(UniAsserter asserter) {
        asserter.assertThat(() -> sessionFactory.withSession(s -> s
                .createQuery("from Hero h where h.name = :name", Hero.class).setParameter("name", "Galadriel")
                .getResultList()),
                list -> assertThat(list).hasSize(1));
    }

    @Entity(name = "Hero")
    @Table(name = Hero.TABLE)
    public static class Hero {

        public static final String TABLE = "Hero_for_DataInitScriptNoneTest";

        @jakarta.persistence.Id
        public java.lang.Long id;

        @Column(unique = true)
        public String name;

    }

}
