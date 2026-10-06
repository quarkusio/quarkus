package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the "update" schema management strategy,
 * the data init script (data.sql) is executed once the schema has been updated,
 * while the schema init script (import.sql) is not, since Hibernate ORM did not create the schema.
 */
public class DataInitScriptWithSchemaManagementUpdateTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "update");

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
