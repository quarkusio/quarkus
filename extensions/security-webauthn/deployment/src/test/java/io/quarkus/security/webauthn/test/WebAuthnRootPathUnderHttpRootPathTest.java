package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;

public class WebAuthnRootPathUnderHttpRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.root-path=/app
                            quarkus.http.non-application-root-path=/internal
                            quarkus.webauthn.root-path=passkeys
                            """), "application.properties"));

    @Test
    public void testPathResolvedAgainstHttpRootPath() {
        RestAssured.given().basePath("").get("/app/passkeys/webauthn.js").then().statusCode(200)
                .body(Matchers.startsWith("\"use strict\";"));
        RestAssured.given().basePath("").get("/app/app/passkeys/webauthn.js").then().statusCode(404);
        RestAssured.given().basePath("").get("/internal/passkeys/webauthn.js").then().statusCode(404);
        RestAssured.given().basePath("").get("/app/q/webauthn/webauthn.js").then().statusCode(404);
    }
}
