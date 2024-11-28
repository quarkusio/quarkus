package io.quarkus.elytron.security.ldap;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AdditionalAttributesTest extends LdapSecurityRealmTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("additional-attributes/application.properties", "application.properties"));
}
