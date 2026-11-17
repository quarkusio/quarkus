package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class WebAuthnHttpRootPathTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.root-path=/app
                            quarkus.webauthn.enable-login-endpoint=true
                            """), "application.properties"));

    @Test
    public void testEndpointsFollowHttpRootPath() {
        RestAssured.given().basePath("").get("/app/q/webauthn/webauthn.js").then().statusCode(200)
                .body(Matchers.startsWith("\"use strict\";"));
        RestAssured
                .given()
                .basePath("")
                .contentType(ContentType.JSON)
                .queryParam("username", "foo")
                .get("/app/q/webauthn/register-options-challenge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("rp.id", Matchers.equalTo("localhost"));
        RestAssured.given().basePath("").post("/app/q/webauthn/login").then().statusCode(400);
        // the registration endpoint is disabled by default
        RestAssured.given().basePath("").post("/app/q/webauthn/register").then().statusCode(404);
        RestAssured.given().basePath("").get("/app/app/q/webauthn/webauthn.js").then().statusCode(404);
        RestAssured.given().basePath("").get("/q/webauthn/webauthn.js").then().statusCode(404);
    }
}
