package io.quarkus.resteasy.reactive.server.test.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EagerAuthSecurityEventTest extends AbstractSecurityEventTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(TEST_CLASSES));

}
