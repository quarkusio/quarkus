package io.quarkus.security.jpa.reactive;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipleRolesInCollectionConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(MultipleRolesInCollectionUserEntity.class)
                    .addAsResource("multiple-roles-in-collection/import.sql", "import.sql")
                    .addAsResource("multiple-roles-in-collection/application.properties", "application.properties"));

}
