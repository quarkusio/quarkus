package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BcryptPasswordMapperTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(BCryptUserEntity.class)
                    .addAsResource("bcrypt-password-mapper/import.sql", "import.sql")
                    .addAsResource("bcrypt-password-mapper/application.properties", "application.properties"));

}
