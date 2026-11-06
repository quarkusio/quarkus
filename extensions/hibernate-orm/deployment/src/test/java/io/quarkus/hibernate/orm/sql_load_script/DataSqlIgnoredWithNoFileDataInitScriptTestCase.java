package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Setting the data init script to "no-file" ignores "data.sql",
 * so an "import.sql" file next to it is no longer ambiguous and keeps the deprecated "sql-load-script" behavior.
 */
public class DataSqlIgnoredWithNoFileDataInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "no-file");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void importSqlExecuted() {
        assertThat(entityName(1)).isEqualTo("default sql load script entity");
    }

    @Test
    public void dataSqlIgnored() {
        assertThat(entityName(10)).isNull();
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
