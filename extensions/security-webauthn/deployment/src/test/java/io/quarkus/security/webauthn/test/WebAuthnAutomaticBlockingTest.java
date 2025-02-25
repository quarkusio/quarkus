package io.quarkus.security.webauthn.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;

public class WebAuthnAutomaticBlockingTest extends WebAuthnAutomaticTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.webauthn.enable-login-endpoint=true
                            quarkus.webauthn.enable-registration-endpoint=true
                            """), "application.properties")
                    .addClasses(WebAuthnBlockingTestUserProvider.class, WebAuthnTestUserProvider.class, WebAuthnHardware.class,
                            TestResource.class, TestUtil.class));
}
