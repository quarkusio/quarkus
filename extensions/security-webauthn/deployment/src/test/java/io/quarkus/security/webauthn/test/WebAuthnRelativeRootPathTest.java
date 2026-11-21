package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;

public class WebAuthnRelativeRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset("quarkus.http.non-application-root-path=/internal\n"
                            + "quarkus.webauthn.root-path=passkeys"), "application.properties"));

    @Test
    public void testPathResolvedAgainstNonApplicationRootPath() {
        RestAssured.get("/internal/passkeys/webauthn.js").then().statusCode(200)
                .body(Matchers.startsWith("\"use strict\";"));
        RestAssured.get("/internal/webauthn/webauthn.js").then().statusCode(404);
    }
}
