package io.quarkus.hibernate.orm.data_management;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class MultipleDataInitScriptsFileAbsentTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application-multiple-data-init-scripts-test.properties", "application.properties")
                    .addAsResource("import-multiple-load-scripts-1.sql", "import-1.sql"));

    @Test
    public void testMultipleDataInitScriptsFileAbsent() {
        // should not be called, deployment exception should happen first:
        // it's illegal to have the data init script configuration property set
        // to an absent file
        Assertions.fail();
    }
}
