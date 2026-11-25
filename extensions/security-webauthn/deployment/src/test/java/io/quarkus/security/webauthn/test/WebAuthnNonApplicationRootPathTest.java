package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;

public class WebAuthnNonApplicationRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.root-path=/app
                            quarkus.http.non-application-root-path=/b
                            """), "application.properties"));

    @Test
    public void testEndpointsIgnoreNonApplicationRootPath() {
        RestAssured.given().basePath("").get("/app/q/webauthn/webauthn.js").then().statusCode(200)
                .body(Matchers.startsWith("\"use strict\";"));
        RestAssured.given().basePath("").get("/b/webauthn/webauthn.js").then().statusCode(404);
        RestAssured.given().basePath("").get("/app/b/webauthn/webauthn.js").then().statusCode(404);
    }
}
