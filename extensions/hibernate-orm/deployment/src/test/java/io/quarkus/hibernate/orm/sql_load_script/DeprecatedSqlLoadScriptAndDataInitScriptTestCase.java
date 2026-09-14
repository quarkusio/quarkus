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
                            "Both 'quarkus.hibernate-orm.data-management.init-script' and 'quarkus.hibernate-orm.sql-load-script' are set.",
                            "'quarkus.hibernate-orm.sql-load-script' is deprecated: remove it and only use 'quarkus.hibernate-orm.data-management.init-script'."));

    @Test
    public void testBothPropertiesSet() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
