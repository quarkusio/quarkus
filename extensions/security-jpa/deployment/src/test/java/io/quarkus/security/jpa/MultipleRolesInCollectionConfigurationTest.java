package io.quarkus.security.jpa;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleRolesInCollectionConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MultipleRolesInCollectionUserEntity.class)
                    .addAsResource("multiple-roles-in-collection/import.sql", "import.sql")
                    .addAsResource("multiple-roles-in-collection/application.properties", "application.properties"));

}
