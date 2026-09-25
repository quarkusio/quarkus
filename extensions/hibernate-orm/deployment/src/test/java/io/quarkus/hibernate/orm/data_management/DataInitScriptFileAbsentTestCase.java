package io.quarkus.hibernate.orm.data_management;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class DataInitScriptFileAbsentTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application-other-data-init-script-test.properties", "application.properties"));

    @Test
    public void testDataInitScriptFileAbsent() {
        // should not be called, deployment exception should happen first:
        // it's illegal to have the data init script configuration property set
        // to an absent file
        Assertions.fail();
    }
}
