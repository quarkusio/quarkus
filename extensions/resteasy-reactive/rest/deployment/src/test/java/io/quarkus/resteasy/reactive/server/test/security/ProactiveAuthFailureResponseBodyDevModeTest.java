package io.quarkus.resteasy.reactive.server.test.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class ProactiveAuthFailureResponseBodyDevModeTest extends AbstractAuthFailureResponseBodyDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class, FailingAuthenticator.class, AuthFailure.class));

}
