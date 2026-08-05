package io.quarkus.security.webauthn.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;

public class WebAuthnAutomaticNonBlockingTest extends WebAuthnAutomaticTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.webauthn.enable-login-endpoint=true
                            quarkus.webauthn.enable-registration-endpoint=true
                            """), "application.properties")
                    .addClasses(WebAuthnNonBlockingTestUserProvider.class, WebAuthnTestUserProvider.class,
                            WebAuthnHardware.class,
                            TestResource.class, TestUtil.class));
}
