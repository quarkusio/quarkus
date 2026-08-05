package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class CustomIdentityProviderConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(PlainUserEntity.class)
                    .addClass(UserEntityIdentityProvider.class)
                    .addAsResource("minimal-config/import.sql", "import.sql")
                    .addAsResource("minimal-config/application.properties", "application.properties"));

}
