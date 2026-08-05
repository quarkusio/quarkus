package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class NamedPersistenceUnitTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MinimalUserEntity.class)
                    .addAsResource("named-persistence-unit/import.sql", "import.sql")
                    .addAsResource("named-persistence-unit/application.properties", "application.properties"));

}
