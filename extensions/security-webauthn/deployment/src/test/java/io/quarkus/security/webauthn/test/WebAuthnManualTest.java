package io.quarkus.security.webauthn.test;

import java.util.List;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.webauthn.WebAuthnController;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.webauthn.Authenticator;

public class WebAuthnManualTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, WebAuthnHardware.class,
                            TestResource.class, ManualResource.class, TestUtil.class));

    @Inject
    WebAuthnUserProvider userProvider;

    @Test
    public void test() throws Exception {

        RestAssured.get("/open").then().statusCode(200).body(Matchers.is("Hello"));
        RestAssured
                .given().redirects().follow(false)
                .get("/secure").then().statusCode(302);
        RestAssured
                .given().redirects().follow(false)
                .get("/admin").then().statusCode(302);
        RestAssured
                .given().redirects().follow(false)
                .get("/cheese").then().statusCode(302);

        Assertions.assertTrue(userProvider.findWebAuthnCredentialsByUserName("stef").await().indefinitely().isEmpty());
        CookieFilter cookieFilter = new CookieFilter();
        String challenge = WebAuthnEndpointHelper.invokeRegistration("stef", cookieFilter);
        WebAuthnHardware hardwareKey = new WebAuthnHardware();
        JsonObject registration = hardwareKey.makeRegistrationJson(challenge);

        // now finalise
        RequestSpecification request = RestAssured
                .given()
                .filter(cookieFilter);
        WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registration);
        request
                .post("/register")
                .then().statusCode(200)
                .body(Matchers.is("OK"))
                .cookie(WebAuthnController.CHALLENGE_COOKIE, Matchers.is(""))
                .cookie(WebAuthnController.USERNAME_COOKIE, Matchers.is(""))
                .cookie("quarkus-credential", Matchers.notNullValue());

        // make sure we stored the user
        List<Authenticator> users = userProvider.findWebAuthnCredentialsByUserName("stef").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stef"));
        Assertions.assertEquals(1, users.get(0).getCounter());

        // make sure our login cookie works
        checkLoggedIn(cookieFilter);

        // reset cookies for the login phase
        cookieFilter = new CookieFilter();
        // now try to log in
        challenge = WebAuthnEndpointHelper.invokeLogin("stef", cookieFilter);
        JsonObject login = hardwareKey.makeLoginJson(challenge);

        // now finalise
        request = RestAssured
                .given()
                .filter(cookieFilter);
        WebAuthnEndpointHelper.addWebAuthnLoginFormParameters(request, login);
        request
                .post("/login")
                .then().statusCode(200)
                .body(Matchers.is("OK"))
                .cookie(WebAuthnController.CHALLENGE_COOKIE, Matchers.is(""))
                .cookie(WebAuthnController.USERNAME_COOKIE, Matchers.is(""))
                .cookie("quarkus-credential", Matchers.notNullValue());

        // make sure we bumped the user
        users = userProvider.findWebAuthnCredentialsByUserName("stef").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stef"));
        Assertions.assertEquals(2, users.get(0).getCounter());

        // make sure our login cookie still works
        checkLoggedIn(cookieFilter);
    }

    private void checkLoggedIn(CookieFilter cookieFilter) {
        RestAssured
                .given()
                .filter(cookieFilter)
                .get("/secure")
                .then()
                .statusCode(200)
                .body(Matchers.is("stef: [admin]"));
        RestAssured
                .given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .get("/admin").then().statusCode(200).body(Matchers.is("OK"));
        RestAssured
                .given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .get("/cheese").then().statusCode(403);
    }
}
