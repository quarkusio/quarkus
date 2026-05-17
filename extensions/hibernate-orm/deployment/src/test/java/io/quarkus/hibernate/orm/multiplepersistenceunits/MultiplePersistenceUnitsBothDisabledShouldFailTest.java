package io.quarkus.hibernate.orm.multiplepersistenceunits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsBothDisabledShouldFailTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(t -> {
                Assertions.assertInstanceOf(ConfigurationException.class, t);
                String msg = t.getMessage();
                Assertions.assertTrue(
                        msg.contains("disables both JDBC and reactive bootstrapping")
                                && msg.contains("quarkus.hibernate-orm.users.jdbc.enabled")
                                && msg.contains("quarkus.hibernate-orm.users.reactive.enabled"),
                        "Unexpected exception message: " + msg);
            })
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-multiple-persistence-units-both-disabled.properties",
                            "application.properties"));

    @Test
    public void shouldFailAtBuildTime() {
        Assertions.fail("Build should have failed before this test method runs.");
    }
}