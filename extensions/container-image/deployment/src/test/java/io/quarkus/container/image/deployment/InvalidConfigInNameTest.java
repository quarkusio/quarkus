package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidConfigInNameTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(NoSuchElementException.class)
            .withEmptyApplication()
            .overrideConfigKey("quarkus.container-image.build", "true")
            .overrideConfigKey("quarkus.container-image.name", "test-${foo.bar}");

    @Test
    void shouldThrow() {
        fail("Should not be run");
    }
}
