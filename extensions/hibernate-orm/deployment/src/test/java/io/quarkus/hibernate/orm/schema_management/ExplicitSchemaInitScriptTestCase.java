package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An explicit schema init script replaces the default one (import.sql)
 * and is executed right after Hibernate ORM created the schema.
 */
public class ExplicitSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("schema-init.sql")
                    .addClasses(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init.sql");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void explicitSchemaInitScriptExecuted() {
        assertThat(entityName(20)).isEqualTo("schema init script entity");
    }

    @Test
    public void defaultSchemaInitScriptNotExecuted() {
        assertThat(entityName(1)).isNull();
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
