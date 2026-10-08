package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Several schema init scripts can be set: they are executed in order after Hibernate ORM created the schema.
 */
public class MultipleSchemaInitScriptsTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema-init-create-table.sql")
                    // Relies on the table created by the previous script
                    .addAsResource(new StringAsset("INSERT INTO MyExtraTable(id) VALUES(2);"),
                            "schema-init-insert-into-table.sql")
                    .addClasses(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script",
                    "schema-init-create-table.sql, schema-init-insert-into-table.sql");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void schemaInitScriptsExecutedInOrder() {
        Long extraTableRows = sessionFactory.fromTransaction(session -> session
                .createNativeQuery("SELECT COUNT(*) FROM MyExtraTable", Long.class)
                .getSingleResult());
        assertThat(extraTableRows).isEqualTo(2L);
    }
}
