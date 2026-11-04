package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Setting "sql-load-script" to "no-file" ignores "import.sql",
 * so a "data.sql" file next to it is no longer ambiguous and gets executed.
 */
public class ImportSqlIgnoredWithNoFileSqlLoadScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "no-file");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void importSqlIgnored() {
        assertThat(entityName(1)).isNull();
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
