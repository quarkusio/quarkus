package io.quarkus.hibernate.orm.multiplepersistenceunits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsUndefinedPackagesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addAsResource("application-multiple-persistence-units-undefined-packages.properties",
                            "application.properties"));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
