package io.quarkus.resteasy.test.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class EagerAuthSecurityEventTest extends AbstractSecurityEventTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TEST_CLASSES));

}
