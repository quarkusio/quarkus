package io.quarkus.security.jpa.reactive;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class BcryptPasswordMapperTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(BCryptUserEntity.class)
                    .addAsResource("bcrypt-password-mapper/import.sql", "import.sql")
                    .addAsResource("bcrypt-password-mapper/application.properties", "application.properties"));

}
