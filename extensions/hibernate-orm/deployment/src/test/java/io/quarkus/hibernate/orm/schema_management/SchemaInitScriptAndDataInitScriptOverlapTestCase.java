package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A given file cannot be both a schema init script and a data init script.
 */
public class SchemaInitScriptAndDataInitScriptOverlapTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("schema-init.sql"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init.sql")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "schema-init.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "'schema-init.sql' is referenced in both 'quarkus.hibernate-orm.data-management.init-script'"
                                    + " and 'quarkus.hibernate-orm.schema-management.init-script'.",
                            "A file must either load data or complete the schema, not both."));

    @Test
    public void testOverlap() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
