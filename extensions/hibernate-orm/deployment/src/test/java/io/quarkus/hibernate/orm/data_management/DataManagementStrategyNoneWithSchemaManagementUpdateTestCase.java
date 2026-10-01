package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the "none" data management strategy, the data init script (data.sql) is not executed on startup,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls
 * for every schema management strategy that does not make Hibernate ORM execute it on start.
 * This test covers "update"; see {@link DataManagementStrategyNoneWithSchemaManagementNoneTestCase} for "none".
 */
public class DataManagementStrategyNoneWithSchemaManagementUpdateTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "update")
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptNotExecutedOnStartupButAvailableToSchemaManager() {
        assertThat(entityName(10)).isNull();

        sessionFactory.getSchemaManager().populate();

        assertThat(entityName(10)).isEqualTo("data.sql data init script entity");
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
