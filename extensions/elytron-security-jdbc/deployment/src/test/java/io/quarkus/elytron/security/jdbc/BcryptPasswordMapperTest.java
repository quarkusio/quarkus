package io.quarkus.elytron.security.jdbc;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class BcryptPasswordMapperTest extends JdbcSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("bcrypt-password-mapper/import.sql")
                    .addAsResource("bcrypt-password-mapper/application.properties", "application.properties"));

}
