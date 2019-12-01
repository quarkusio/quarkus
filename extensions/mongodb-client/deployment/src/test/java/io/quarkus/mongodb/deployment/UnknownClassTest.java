package io.quarkus.mongodb.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class UnknownClassTest {

    @RegisterExtension
    static final QuarkusUnitTest unknownClassConfig = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.mongodb.ssl-context-class", "io.quarkus.UnknownClass")
            .assertException(t -> {
                assertEquals(IllegalArgumentException.class, t.getClass());
                assertEquals("Impossible to find class : 'io.quarkus.UnknownClass' to set the SSLContext", t.getMessage());
            });

    @Test
    public void configTest() {
        // should not be called, an exception should happen first:
        Assertions.fail();
    }
}
