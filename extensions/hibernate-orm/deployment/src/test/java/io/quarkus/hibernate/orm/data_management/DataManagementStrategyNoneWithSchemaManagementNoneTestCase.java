package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the "none" data management strategy and a schema managed by another tool (schema management strategy "none"),
 * the data init script (data.sql) is not executed on startup,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls.
 */
public class DataManagementStrategyNoneWithSchemaManagementNoneTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class,
                            PreexistingSchemaH2Database.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("data-management-none-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
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
