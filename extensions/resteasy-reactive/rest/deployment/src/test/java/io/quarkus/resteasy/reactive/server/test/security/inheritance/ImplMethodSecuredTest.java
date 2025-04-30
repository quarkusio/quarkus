package io.quarkus.resteasy.reactive.server.test.security.inheritance;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ImplMethodSecuredTest extends AbstractImplMethodSecuredTest {

    @RegisterExtension
    static QuarkusUnitTest runner = getRunner();

}
