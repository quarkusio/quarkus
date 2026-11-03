package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The deprecated "sql-load-script" property next to a "data.sql" file,
 * which is picked up by its replacement, is ambiguous.
 */
public class DeprecatedSqlLoadScriptAndDataSqlTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "import.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "('quarkus.hibernate-orm.sql-load-script' is set)",
                            "(the file 'data.sql' exists in the classpath)",
                            "To use the newer SQL init script behavior, remove 'quarkus.hibernate-orm.sql-load-script'"
                                    + " and use 'quarkus.hibernate-orm.data-management.init-script'"
                                    + " or 'quarkus.hibernate-orm.schema-management.init-script' instead,"
                                    + " and move the statements of 'import.sql' to 'data.sql' if they load data"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
