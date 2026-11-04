package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The deprecated "sql-load-script" property cannot be combined with its replacement.
 */
public class DeprecatedSqlLoadScriptAndDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("import.sql")
                    .addAsResource("data.sql"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "import.sql")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "data.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' would use both the deprecated SQL load script behavior"
                                    + " ('quarkus.hibernate-orm.sql-load-script' is set)"
                                    + " and the newer SQL init script behavior that replaces it ('quarkus.hibernate-orm.data-management.init-script' is set),"
                                    + " which is ambiguous.",
                            "To use the newer SQL init script behavior, remove 'quarkus.hibernate-orm.sql-load-script'",
                            "To keep the deprecated behavior, set 'quarkus.hibernate-orm.data-management.init-script' to 'no-file'."));

    @Test
    public void testBothPropertiesSet() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
