package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

public class WebAuthnTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class));

    @TestHTTPResource
    public String url;

    @Test
    public void testJavaScriptFile() {
        RestAssured.get("/q/webauthn/webauthn.js").then().statusCode(200).body(Matchers.startsWith("\"use strict\";"));
    }

    @Test
    public void testLoginRpFromFirstOrigin() {
        RestAssured
                .given()
                .body(new JsonObject()
                        .put("name", "foo").encode())
                .contentType(ContentType.JSON)
                .post("/q/webauthn/register-options-challenge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("rp.id", Matchers.equalTo("localhost"));
    }

    @Test
    public void testRegisterChallengeIsEqualAcrossCalls() {
        CookieFilter cookieFilter = new CookieFilter();

        String challenge = RestAssured
                .given()
                .filter(cookieFilter)
                .body(new JsonObject()
                        .put("name", "foo").encode())
                .contentType(ContentType.JSON)
                .post("/q/webauthn/register-options-challenge")
                .jsonPath().get("challenge");

        RestAssured
                .given()
                .filter(cookieFilter)
                .body(new JsonObject()
                        .put("name", "foo").encode())
                .contentType(ContentType.JSON)
                .post("/q/webauthn/register-options-challenge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("challenge", Matchers.equalTo(challenge));
    }

    @Test
    public void testLoginChallengeIsEqualAcrossCalls() {
        CookieFilter cookieFilter = new CookieFilter();

        String challenge = RestAssured
                .given()
                .filter(cookieFilter)
                .body(new JsonObject().encode())
                .contentType(ContentType.JSON)
                .post("/q/webauthn/login-options-challenge")
                .jsonPath().get("challenge");

        RestAssured
                .given()
                .filter(cookieFilter)
                .body(new JsonObject().encode())
                .contentType(ContentType.JSON)
                .post("/q/webauthn/login-options-challenge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("challenge", Matchers.equalTo(challenge));
    }

    @Test
    public void testWellKnownDefault() {
        String origin = url;
        if (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        RestAssured.get("/.well-known/webauthn").then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("origins.size()", Matchers.equalTo(1))
                .body("origins[0]", Matchers.equalTo(origin));
    }
}
