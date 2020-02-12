package io.quarkus.security.jpa;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NaturalIdConfigurationTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addClass(NaturalIdUserEntity.class)
                    .addAsResource("minimal-config/import.sql", "import.sql")
                    .addAsResource("minimal-config/application.properties", "application.properties"));

}
