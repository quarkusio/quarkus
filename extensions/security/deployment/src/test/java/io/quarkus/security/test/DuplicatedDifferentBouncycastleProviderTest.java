package io.quarkus.security.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DuplicatedDifferentBouncycastleProviderTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-duplicated-different-providers.properties", "application.properties"));

    @Test
    void shouldThrow() {
        Assertions.fail("An IllegalStateException should have been thrown due to duplicated bouncycastle providers");
    }
}
