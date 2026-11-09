package io.quarkus.hibernate.orm.schema_management;

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
import io.quarkus.hibernate.orm.data_management.PreexistingSchemaH2Database;
import io.quarkus.hibernate.orm.data_management.SchemaManagementScenario;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An explicit schema init script replaces the default one (schema.sql)
 * and is executed right after Hibernate ORM created the schema:
 * it is not executed when Hibernate ORM does not create the schema.
 */
@ParameterizedClass
@EnumSource(SchemaManagementScenario.class)
public class ExplicitSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema.sql")
                    .addAsResource("schema-init.sql")
                    .addClasses(MyEntity.class, PreexistingSchemaH2Database.class, SchemaManagementScenario.class));

    @Parameter
    SchemaManagementScenario scenario;

    @BeforeParameterizedClassInvocation
    static void configure(SchemaManagementScenario scenario) {
        scenario.configure(runner.resetForParameterizedClass(), "schema-init-script-schema-none");
        runner.overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init.sql");
    }

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void explicitSchemaInitScriptExecutedOnlyWhenSchemaCreated() {
        if (scenario.executesSchemaInitScript) {
            assertThat(entityName(20)).isEqualTo("schema init script entity");
        } else {
            assertThat(entityName(20)).isNull();
        }
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
