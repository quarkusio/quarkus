package io.quarkus.security.test;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests of a CUSTOM authentication mechanism that uses the BASIC authentication headers
 */
public class CustomAuthEmbeddedEncryptedTestCase extends CustomAuthEmbeddedBase {
    static Class[] testClasses = {
            TestSecureServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
            ParametrizedPathsResource.class, SubjectExposingResource.class
    };
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClasses(CustomAuth.class)
                    .addAsResource("application-custom-auth-embedded-encrypted.properties",
                            "application.properties"));
}
