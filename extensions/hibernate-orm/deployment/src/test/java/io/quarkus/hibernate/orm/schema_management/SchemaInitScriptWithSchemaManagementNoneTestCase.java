package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.data_management.PreexistingSchemaH2Database;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The schema init script is only executed when Hibernate ORM creates the schema:
 * it is not executed when the schema is managed by another tool (schema management strategy "none").
 */
public class SchemaInitScriptWithSchemaManagementNoneTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema-init.sql")
                    .addClasses(MyEntity.class, PreexistingSchemaH2Database.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("schema-init-script-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init.sql");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void schemaInitScriptNotExecuted() {
        assertThat(entityName(20)).isNull();
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
