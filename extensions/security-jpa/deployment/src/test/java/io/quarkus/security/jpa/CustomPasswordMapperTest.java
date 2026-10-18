package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class CustomPasswordMapperTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClasses(CustomPasswordUserEntity.class, CustomPasswordProvider.class)
                    .addAsResource("custom-password-mapper/import.sql", "import.sql")
                    .addAsResource("custom-password-mapper/application.properties", "application.properties"));

}
