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
 * With the "none" data management strategy, the data init script (data.sql) is not executed on startup,
 * whatever the schema management strategy,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls.
 */
@ParameterizedClass
@EnumSource(SchemaManagementScenario.class)
public class DataManagementStrategyNoneTestCase {

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
        scenario.configure(runner.resetForParameterizedClass(), "data-management-none-schema-none");
        runner.overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");
    }

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptNotExecutedOnStartupButAvailableToSchemaManager() {
        if (scenario.executesSchemaInitScript) {
            // The schema init script was executed as part of the schema creation, the data init script was not
            assertThat(entityName(1)).isEqualTo("default schema init script entity");
        } else {
            assertThat(entityName(1)).isNull();
        }
        assertThat(entityName(10)).isNull();

        // populate() executes the data init script
        sessionFactory.getSchemaManager().populate();
        assertThat(entityName(10)).isEqualTo("data.sql data init script entity");

        // truncate() clears the tables, then executes the data init script again
        sessionFactory.getSchemaManager().truncate();
        assertThat(entityName(1)).isNull();
        assertThat(entityName(10)).isEqualTo("data.sql data init script entity");
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
