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
 * With the "update" schema management strategy, the data init script (data.sql)
 * is executed once the schema has been updated.
 */
public class DataInitScriptWithSchemaManagementUpdateTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("data-init-script-test.sql", "data.sql"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "update");

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

        public static final String TABLE = "Hero_for_DataInitScriptTest";

        @jakarta.persistence.Id
        public java.lang.Long id;

        @Column(unique = true)
        public String name;

    }

}
