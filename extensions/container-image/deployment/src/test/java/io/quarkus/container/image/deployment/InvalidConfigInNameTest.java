package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValidationException;

public class InvalidConfigInNameTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(t -> {
                assertTrue(t instanceof ConfigValidationException);
                assertTrue(t.getMessage().contains("NoSuchElementException"));
            })
            .withEmptyApplication()
            .overrideConfigKey("quarkus.container-image.build", "true")
            .overrideConfigKey("quarkus.container-image.name", "test-${foo.bar}");

    @Test
    void shouldThrow() {
        fail("Should not be run");
    }
}
