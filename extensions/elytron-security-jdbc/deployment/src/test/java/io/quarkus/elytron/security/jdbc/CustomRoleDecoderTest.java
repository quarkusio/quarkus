package io.quarkus.elytron.security.jdbc;

import java.util.Arrays;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CustomRoleDecoderTest extends JdbcSecurityRealmTest {

    static Class[] testClassesWithCustomRoleDecoder = Stream.concat(
            Arrays.stream(testClasses),
            Arrays.stream(new Class[] { CustomRoleDecoder.class })).toArray(Class[]::new);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClassesWithCustomRoleDecoder)
                    .addAsResource("custom-role-decoder/import.sql")
                    .addAsResource("custom-role-decoder/application.properties", "application.properties"));

}
