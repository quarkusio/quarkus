package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Using "import.sql" as the data init script is ambiguous, since that file also enables
 * the deprecated "sql-load-script" behavior: the data init script should be renamed instead.
 */
public class ImportSqlAndDataInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "import.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "(the file 'import.sql' exists in the classpath)",
                            "('quarkus.hibernate-orm.data-management.init-script' is set)",
                            "move the statements of 'import.sql' to 'data.sql' if they load data",
                            "(updating any property that refers to it)"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
