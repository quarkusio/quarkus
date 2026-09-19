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
                            "'quarkus.hibernate-orm.sql-load-script' is deprecated and cannot be used together with"
                                    + " 'quarkus.hibernate-orm.data-management.init-script' or 'quarkus.hibernate-orm.schema-management.init-script'.",
                            "Remove it and only use those properties."));

    @Test
    public void testBothPropertiesSet() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
