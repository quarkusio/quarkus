package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PanacheEntitiesConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(PanacheUserEntity.class)
                    .addClass(PanacheRoleEntity.class)
                    .addAsResource("multiple-entities/import.sql", "import.sql")
                    .addAsResource("multiple-entities/application.properties", "application.properties"));

}
