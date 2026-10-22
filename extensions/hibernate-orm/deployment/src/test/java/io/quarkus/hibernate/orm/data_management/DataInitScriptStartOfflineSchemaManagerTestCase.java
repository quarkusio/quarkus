package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When starting offline, the data init script (data.sql) is not executed on startup,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls
 * once the database can be reached.
 */
public class DataInitScriptStartOfflineSchemaManagerTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("data.sql"))
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:start-offline-schema-manager;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptAvailableToSchemaManager() {
        // create() executes the data init script right after creating the schema, like on startup
        sessionFactory.getSchemaManager().create(false);

        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, 10L));
        assertThat(entity).isNotNull()
                .extracting(MyEntity::getName).isEqualTo("data.sql data init script entity");
    }
}
