package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MinimalConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MinimalUserEntity.class)
                    .addAsResource("minimal-config/import.sql", "import.sql")
                    .addAsResource("minimal-config/application.properties", "application.properties"));

}
