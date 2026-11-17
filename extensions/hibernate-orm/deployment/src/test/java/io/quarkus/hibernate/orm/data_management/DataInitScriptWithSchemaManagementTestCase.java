package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The default data init script (data.sql) is executed on startup whatever the schema management strategy,
 * while the schema init script (schema.sql) is executed only when Hibernate ORM creates the schema.
 */
@ParameterizedClass
@EnumSource(SchemaManagementScenario.class)
public class DataInitScriptWithSchemaManagementTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema.sql")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class, PreexistingSchemaH2Database.class, SchemaManagementScenario.class));

    @Parameter
    SchemaManagementScenario scenario;

    @BeforeParameterizedClassInvocation
    static void configure(SchemaManagementScenario scenario) {
        scenario.configure(runner.resetForParameterizedClass(), "data-init-script-schema-none");
    }

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptExecuted() {
        assertThat(entityName(10)).isEqualTo("data.sql data init script entity");
    }

    @Test
    public void schemaInitScriptExecutedOnlyWhenSchemaCreated() {
        if (scenario.executesSchemaInitScript) {
            assertThat(entityName(1)).isEqualTo("default schema init script entity");
        } else {
            assertThat(entityName(1)).isNull();
        }
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
