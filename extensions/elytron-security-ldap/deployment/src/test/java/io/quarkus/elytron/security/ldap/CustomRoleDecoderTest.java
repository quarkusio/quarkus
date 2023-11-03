package io.quarkus.elytron.security.ldap;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CustomRoleDecoderTest extends LdapSecurityRealmTest {

    static Class[] testClassesWithCustomRoleDecoder = Stream.concat(
            Arrays.stream(testClasses),
            Arrays.stream(new Class[] { CustomRoleDecoder.class })).toArray(Class[]::new);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClassesWithCustomRoleDecoder)
                    .addAsResource("custom-role-decoder/application.properties", "application.properties"));

}
