package io.quarkus.security.jpa;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CustomPasswordMapperTest extends JpaSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addClasses(CustomPasswordUserEntity.class, CustomPasswordProvider.class)
                    .addAsResource("custom-password-mapper/import.sql", "import.sql")
                    .addAsResource("custom-password-mapper/application.properties", "application.properties"));

}
