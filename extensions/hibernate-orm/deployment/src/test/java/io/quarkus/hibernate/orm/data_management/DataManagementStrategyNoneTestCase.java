package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the "none" data management strategy while Hibernate ORM creates the schema (the default in tests),
 * the data init script (data.sql) is not executed on startup, although the schema init script (import.sql) is,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls.
 */
public class DataManagementStrategyNoneTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataInitScriptNotExecutedOnStartupButAvailableToSchemaManager() {
        // The schema init script was executed as part of the schema creation, the data init script was not
        assertThat(entityName(1)).isEqualTo("default sql load script entity");
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
