package io.quarkus.security.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

class DuplicateRealmNameTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestApplication.class)
                    .addAsResource("test-users.properties")
                    .addAsResource("test-roles.properties")
                    .addAsResource(new StringAsset("""
                            quarkus.security.users.file.enabled=true
                            quarkus.security.users.file.users=test-users.properties
                            quarkus.security.users.file.roles=test-roles.properties
                            quarkus.security.users.file.plain-text=true
                            quarkus.security.users.embedded.enabled=true
                            quarkus.security.users.embedded.plain-text=true
                            quarkus.security.users.embedded.users.scott=jb0ss
                            quarkus.security.users.embedded.roles.scott=admin
                            """), "application.properties"))
            .assertException(t -> {
                Throwable cause = t.getCause();
                Assertions.assertInstanceOf(ConfigurationException.class, cause);
                String message = cause.getMessage();
                Assertions.assertTrue(message.contains("realm name 'Quarkus'"), message);
                Assertions.assertTrue(message.contains("quarkus.security.users.file.realm-name"), message);
                Assertions.assertTrue(message.contains("quarkus.security.users.embedded.realm-name"), message);
            });

    @Test
    void testDuplicateRealmNamesAreRejected() {
        // the build must fail, see assertException above
    }
}
