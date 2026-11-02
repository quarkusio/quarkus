package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An "import.sql" file enables the deprecated "sql-load-script" behavior,
 * which cannot be combined with the data management strategy, a runtime property.
 */
public class DefaultSqlLoadScriptAndDataManagementStrategyTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "create")
            .assertException(t -> assertThat(t)
                    .hasMessageContaining(
                            "The file 'import.sql' in the classpath enables the deprecated behavior of"
                                    + " 'quarkus.hibernate-orm.sql-load-script',"
                                    + " which cannot be used together with 'quarkus.hibernate-orm.data-management.strategy'."
                                    + " Move its statements to 'data.sql' if they load data and/or to 'schema.sql'"
                                    + " if they complete the schema, then delete it."));

    @Test
    public void applicationStarts() {
        Assertions.fail("Startup should have failed");
    }
}
