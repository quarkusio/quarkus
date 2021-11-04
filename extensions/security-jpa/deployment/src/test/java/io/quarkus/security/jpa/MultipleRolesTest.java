package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleRolesTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MultipleRolesUserEntity.class)
                    .addAsResource("multiple-roles/import.sql", "import.sql")
                    .addAsResource("multiple-roles/application.properties", "application.properties"));

}
