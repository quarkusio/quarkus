package io.quarkus.mongodb.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NotCompliantClassTest {

    @RegisterExtension
    static final QuarkusUnitTest classNotCompliantConfig = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.mongodb.ssl-context-class",
                    "io.quarkus.mongodb.deployment.NotCompliantCustomSSLContext")
            .assertException(t -> {
                assertEquals(IllegalArgumentException.class, t.getClass());
                assertEquals(
                        "The class you specified in quarkus.mongodb.ssl-context-class does not implement io.quarkus.mongodb.runtime.SSLContextConfig",
                        t.getMessage());
            });

    @Test
    public void configTest() {
        // should not be called, an exception should happen first:
        Assertions.fail();
    }
}
