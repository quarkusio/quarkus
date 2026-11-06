package io.quarkus.security.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that the application fails to start when multiple realms are enabled, but none of them is the default realm.
 */
class UnknownDefaultRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestApplication.class, SubjectExposingResource.class)
                    .addAsResource("application-multiple-realms.properties", "application.properties")
                    .addAsResource("test-users.properties")
                    .addAsResource("test-roles.properties"))
            .overrideConfigKey("quarkus.security.users.file.realm-name", "file")
            .assertException(t -> {
                Throwable cause = t;
                while (cause != null && !(cause instanceof ConfigurationException)) {
                    cause = cause.getCause();
                }
                Assertions.assertNotNull(cause, "Expected ConfigurationException, but got: " + t);
                String message = cause.getMessage();
                Assertions.assertTrue(message.contains("(embedded, file)"), message);
                Assertions.assertTrue(message.contains("'Quarkus'"), message);
                Assertions.assertTrue(message.contains("quarkus.security.elytron.default-realm-name"), message);
            });

    @Test
    void testStartupFails() {
        Assertions.fail("the application must fail to start");
    }
}
