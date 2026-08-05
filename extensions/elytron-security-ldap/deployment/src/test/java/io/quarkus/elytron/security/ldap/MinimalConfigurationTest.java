package io.quarkus.elytron.security.ldap;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MinimalConfigurationTest extends LdapSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("minimal-config/application.properties", "application.properties"));

    protected String expectedStandardUserName() {
        return "standardUser:Standard User";
    }
}
