package io.quarkus.security.jpa;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleRolesInCollectionConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addClass(MultipleRolesInCollectionUserEntity.class)
                    .addAsResource("multiple-roles-in-collection/import.sql", "import.sql")
                    .addAsResource("multiple-roles-in-collection/application.properties", "application.properties"));

}
