package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The deprecated "sql-load-script" property cannot be combined with the data management strategy,
 * which only applies to its replacement.
 */
public class DeprecatedSqlLoadScriptAndDataManagementStrategyTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application-import-load-script-test.properties", "application.properties")
                    .addAsResource("import.sql"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none")
            .assertException(t -> assertThat(t)
                    .hasMessageContaining(
                            "'quarkus.hibernate-orm.sql-load-script' is deprecated and cannot be used together with"
                                    + " 'quarkus.hibernate-orm.data-management.strategy'."
                                    + " Remove it and use 'quarkus.hibernate-orm.data-management.init-script' instead."));

    @Test
    public void applicationStarts() {
        Assertions.fail("Startup has failed");
    }
}
