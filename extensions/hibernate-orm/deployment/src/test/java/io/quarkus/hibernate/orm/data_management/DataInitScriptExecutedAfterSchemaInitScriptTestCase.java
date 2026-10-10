package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When Hibernate ORM creates the schema, the schema init script is executed before the data init script,
 * so that the latter can rely on database objects created by the former.
 */
public class DataInitScriptExecutedAfterSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema-init-create-table.sql")
                    .addAsResource("data-from-extra-table.sql")
                    .addClasses(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init-create-table.sql")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "data-from-extra-table.sql");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptExecutedAfterSchemaInitScript() {
        assertThat(entityName(30)).isEqualTo("data init script entity after schema init script");
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
