package io.quarkus.security.webauthn.test;

import java.net.URL;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

public class WebAuthnAttestationTlsConfigurationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.webauthn.enable-registration-endpoint=true
                            quarkus.webauthn.attestation=direct
                            quarkus.webauthn.tls-configuration-name=attestation
                            quarkus.tls.attestation.trust-store.pem.certs=attestation/ca.pem
                            """), "application.properties")
                    .addAsResource("attestation/ca.pem")
                    .addAsResource("attestation/authenticator.pem")
                    .addAsResource("attestation/authenticator-key.pem")
                    .addAsResource("attestation/untrusted-authenticator.pem")
                    .addAsResource("attestation/untrusted-authenticator-key.pem")
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class,
                            WebAuthnHardware.class, PackedAttestation.class, TestUtil.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testAttestationIssuedByTrustedCa() {
        CookieFilter cookieFilter = new CookieFilter();
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("stef", cookieFilter);
        JsonObject registration = PackedAttestation.TRUSTED.attest(new WebAuthnHardware(url).makeRegistrationJson(challenge));

        WebAuthnEndpointHelper.invokeRegistration("stef", registration, cookieFilter);
    }

    @Test
    public void testAttestationNotIssuedByTrustedCa() {
        CookieFilter cookieFilter = new CookieFilter();
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("scott", cookieFilter);
        JsonObject registration = PackedAttestation.UNTRUSTED
                .attest(new WebAuthnHardware(url).makeRegistrationJson(challenge));

        RestAssured
                .given().body(registration.encode())
                .filter(cookieFilter)
                .contentType(ContentType.JSON)
                .queryParam("username", "scott")
                .post("/q/webauthn/register")
                .then()
                .statusCode(400);
    }
}
