package io.quarkus.security.webauthn.test;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;

public class WebAuthnAutomaticNonBlockingTest extends WebAuthnAutomaticTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnNonBlockingTestUserProvider.class, WebAuthnTestUserProvider.class,
                            WebAuthnHardware.class,
                            TestResource.class, TestUtil.class));
}
