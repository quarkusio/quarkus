package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An "import.sql" file enables the deprecated "sql-load-script" behavior,
 * while a "schema.sql" file is picked up by its replacement: having both is ambiguous.
 */
public class ImportSqlAndSchemaSqlTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("schema.sql"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "(the file 'import.sql' exists in the classpath)",
                            "(the file 'schema.sql' exists in the classpath)",
                            "To keep the deprecated behavior, delete or rename 'schema.sql'"
                                    + " (or set 'quarkus.hibernate-orm.schema-management.init-script' to 'no-file')."));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
