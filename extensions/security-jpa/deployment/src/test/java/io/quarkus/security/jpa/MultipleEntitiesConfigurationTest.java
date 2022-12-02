package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleEntitiesConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(ExternalRolesUserEntity.class)
                    .addClass(RoleEntity.class)
                    .addAsResource("multiple-entities/import.sql", "import.sql")
                    .addAsResource("multiple-entities/application.properties", "application.properties"));

}
