package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When the schema is managed by another tool (schema management strategy "none"),
 * the data init script (data.sql) is still executed on startup,
 * while the schema init script (import.sql) is not, since Hibernate ORM did not create the schema.
 */
public class DataInitScriptWithSchemaManagementNoneTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class, PreexistingSchemaH2Database.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("data-init-script-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptExecuted() {
        assertThat(entityName(10)).isEqualTo("data.sql data init script entity");
    }

    @Test
    public void schemaInitScriptNotExecuted() {
        assertThat(entityName(1)).isNull();
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
