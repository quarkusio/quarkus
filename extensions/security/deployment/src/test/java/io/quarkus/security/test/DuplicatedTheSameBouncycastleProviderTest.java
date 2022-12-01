package io.quarkus.security.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DuplicatedTheSameBouncycastleProviderTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-duplicated-the-same-providers.properties", "application.properties"));

    @Test
    public void testSuccess() {
        //execution of the test is enough to confirm that configuration is allowed
    }
}
