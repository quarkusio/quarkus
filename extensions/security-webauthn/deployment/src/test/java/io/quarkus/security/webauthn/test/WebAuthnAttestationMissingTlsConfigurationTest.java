package io.quarkus.security.webauthn.test;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;

public class WebAuthnAttestationMissingTlsConfigurationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.webauthn.attestation=direct
                            quarkus.webauthn.tls-configuration-name=missing
                            """), "application.properties")
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class));

    @Inject
    WebAuthnSecurity security;

    @Test
    public void testMissingTlsConfiguration() {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, security::getWebAuthn4J);
        Assertions.assertEquals("Unable to find the TLS configuration missing for the WebAuthn attestation trust anchors.",
                e.getMessage());
    }
}
