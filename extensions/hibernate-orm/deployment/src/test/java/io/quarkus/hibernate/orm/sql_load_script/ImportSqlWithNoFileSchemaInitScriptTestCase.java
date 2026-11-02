package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * "import.sql" belongs to the deprecated "sql-load-script" behavior:
 * setting the schema init script to "no-file" does not ignore it.
 */
public class ImportSqlWithNoFileSchemaInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "no-file");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void importSqlExecuted() {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, 1L));
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("default sql load script entity");
    }
}
