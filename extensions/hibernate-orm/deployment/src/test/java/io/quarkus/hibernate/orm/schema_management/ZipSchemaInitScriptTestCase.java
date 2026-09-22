package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Unlike data init scripts, schema init scripts cannot be packaged as zip files.
 */
public class ZipSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("load-script-test.zip"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "load-script-test.zip")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Zip files are not supported in 'quarkus.hibernate-orm.schema-management.init-script=load-script-test.zip'.",
                            "Reference the SQL files directly."));

    @Test
    public void testZip() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
