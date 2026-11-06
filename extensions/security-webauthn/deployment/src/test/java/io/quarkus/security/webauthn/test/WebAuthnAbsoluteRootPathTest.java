package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class WebAuthnAbsoluteRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset("quarkus.webauthn.root-path=/auth/webauthn"),
                            "application.properties"));

    @Test
    public void testEndpointsUnderConfiguredPath() {
        RestAssured.get("/auth/webauthn/webauthn.js").then().statusCode(200)
                .body(Matchers.startsWith("\"use strict\";"));
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .queryParam("username", "foo")
                .get("/auth/webauthn/register-options-challenge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("rp.id", Matchers.equalTo("localhost"));
    }

    @Test
    public void testDefaultPathNotServed() {
        RestAssured.get("/q/webauthn/webauthn.js").then().statusCode(404);
    }
}
