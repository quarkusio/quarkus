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
 * while a "data.sql" file is picked up by its replacement: having both is ambiguous.
 */
public class ImportSqlAndDataSqlTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql"))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' would use both the deprecated SQL load script behavior"
                                    + " (the file 'import.sql' exists in the classpath)"
                                    + " and the newer SQL init script behavior that replaces it (the file 'data.sql' exists in the classpath),"
                                    + " which is ambiguous.",
                            "To use the newer SQL init script behavior, move the statements of 'import.sql' to 'data.sql'"
                                    + " if they load data and/or to 'schema.sql' if they complete the schema,"
                                    + " then delete it (updating any property that refers to it),"
                                    + " or set 'quarkus.hibernate-orm.sql-load-script' to 'no-file'.",
                            "To keep the deprecated behavior, delete or rename 'data.sql'"
                                    + " (or set 'quarkus.hibernate-orm.data-management.init-script' to 'no-file')."));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
