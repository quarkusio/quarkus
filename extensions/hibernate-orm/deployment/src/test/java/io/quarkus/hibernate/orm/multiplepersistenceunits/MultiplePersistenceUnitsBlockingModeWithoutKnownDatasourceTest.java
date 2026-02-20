package io.quarkus.hibernate.orm.multiplepersistenceunits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsBlockingModeWithoutKnownDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addAsResource("application-multiple-persistence-units-mode-blocking-no-known-ds.properties",
                            "application.properties"));

    @Test
    public void buildShouldSucceed() {
        // If this test runs, boot succeeded.
    }
}