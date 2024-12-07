package io.quarkus.security.webauthn.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

public class WebAuthnOriginsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, TestUtil.class)
                    .addAsResource(new StringAsset("quarkus.webauthn.origins=http://foo,https://bar:42"),
                            "application.properties"));

    @Test
    public void testLoginRpFromFirstOrigin() {
        RestAssured
                .given()
                .body(new JsonObject()
                        .put("name", "foo").encode())
                .contentType(ContentType.JSON)
                .post("/q/webauthn/register-options-challenge")
                .then()
                .log().all()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("rp.id", Matchers.equalTo("foo"));
    }

    @Test
    public void testWellKnownConfigured() {
        RestAssured.get("/.well-known/webauthn")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("origins.size()", Matchers.equalTo(2))
                .body("origins[0]", Matchers.equalTo("http://foo"))
                .body("origins[1]", Matchers.equalTo("https://bar:42"));
    }
}
