package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An explicit data init script replaces the default one (data.sql),
 * but does not affect the default schema init script (import.sql).
 */
public class ExplicitDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addAsResource("data-custom.sql")
                    .addClasses(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "data-custom.sql");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void explicitDataInitScriptExecuted() {
        assertThat(entityName(11)).isEqualTo("custom data init script entity");
    }

    @Test
    public void defaultDataInitScriptNotExecuted() {
        assertThat(entityName(10)).isNull();
    }

    @Test
    public void defaultSchemaInitScriptExecuted() {
        assertThat(entityName(1)).isEqualTo("default sql load script entity");
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
