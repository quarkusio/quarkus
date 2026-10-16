package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the default schema management strategy in tests (drop-and-create),
 * both the default schema init script (import.sql) and the default data init script (data.sql) are executed.
 */
public class DefaultDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(MyEntity.class));

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void schemaInitScriptExecuted() {
        assertThat(entityName(1)).isEqualTo("default sql load script entity");
    }

    @Test
    public void dataInitScriptExecuted() {
        assertThat(entityName(10)).isEqualTo("data.sql data init script entity");
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
