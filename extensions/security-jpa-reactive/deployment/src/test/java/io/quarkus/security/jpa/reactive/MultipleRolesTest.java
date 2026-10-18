package io.quarkus.security.jpa.reactive;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipleRolesTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MultipleRolesUserEntity.class)
                    .addAsResource("multiple-roles/import.sql", "import.sql")
                    .addAsResource("multiple-roles/application.properties", "application.properties"));

}
