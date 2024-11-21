package io.quarkus.security.webauthn.test;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

public class WebAuthnManualTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class,
                            WebAuthnTestUserProvider.class, WebAuthnHardware.class,
                            TestResource.class, ManualResource.class, TestUtil.class));

    @Inject
    WebAuthnManualTestUserProvider userProvider;

    @TestHTTPResource
    URL url;

    @BeforeEach
    public void before() {
        userProvider.clear();
    }

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

        Assertions.assertTrue(userProvider.findByUserName("stef").await().indefinitely().isEmpty());
        CookieFilter cookieFilter = new CookieFilter();
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("stef", cookieFilter);
        WebAuthnHardware hardwareKey = new WebAuthnHardware(url);
        JsonObject registration = hardwareKey.makeRegistrationJson(challenge);

        // now finalise
        RequestSpecification request = RestAssured
                .given()
                .filter(cookieFilter);
        WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registration);
        request
                .log().ifValidationFails()
                .queryParam("username", "stef")
                .post("/register")
                .then().statusCode(200)
                .log().ifValidationFails()
                .body(Matchers.is("OK"))
                .cookie("_quarkus_webauthn_challenge", Matchers.is(""))
                .cookie("_quarkus_webauthn_username", Matchers.is(""))
                .cookie("quarkus-credential", Matchers.notNullValue());

        // make sure we stored the user
        List<WebAuthnCredentialRecord> users = userProvider.findByUserName("stef").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stef"));
        Assertions.assertEquals(1, users.get(0).getCounter());

        // make sure our login cookie works
        checkLoggedIn(cookieFilter);

        // reset cookies for the login phase
        cookieFilter = new CookieFilter();
        // now try to log in
        challenge = WebAuthnEndpointHelper.obtainLoginChallenge("stef", cookieFilter);
        JsonObject login = hardwareKey.makeLoginJson(challenge);

        // now finalise
        request = RestAssured
                .given()
                .filter(cookieFilter);
        WebAuthnEndpointHelper.addWebAuthnLoginFormParameters(request, login);
        request
                .log().ifValidationFails()
                .post("/login")
                .then().statusCode(200)
                .log().ifValidationFails()
                .body(Matchers.is("OK"))
                .cookie("_quarkus_webauthn_challenge", Matchers.is(""))
                .cookie("_quarkus_webauthn_username", Matchers.is(""))
                .cookie("quarkus-credential", Matchers.notNullValue());

        // make sure we bumped the user
        users = userProvider.findByUserName("stef").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stef"));
        Assertions.assertEquals(2, users.get(0).getCounter());

        // make sure our login cookie still works
        checkLoggedIn(cookieFilter);

        // make sure we can't log in via the default endpoint
        // reset cookies for the login phase
        CookieFilter finalCookieFilter = new CookieFilter();
        // now try to log in
        challenge = WebAuthnEndpointHelper.obtainLoginChallenge("stef", finalCookieFilter);
        JsonObject defaultLogin = hardwareKey.makeLoginJson(challenge);

        // now finalise
        Assertions.assertThrows(AssertionError.class,
                () -> WebAuthnEndpointHelper.invokeLogin(defaultLogin, finalCookieFilter));

        // make sure we did not bump the user
        users = userProvider.findByUserName("stef").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stef"));
        Assertions.assertEquals(2, users.get(0).getCounter());
    }

    @Test
    public void checkDefaultRegistrationDisabled() {
        Assertions.assertTrue(userProvider.findByUserName("stef").await().indefinitely().isEmpty());
        CookieFilter cookieFilter = new CookieFilter();
        WebAuthnHardware hardwareKey = new WebAuthnHardware(url);
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("stef", cookieFilter);
        JsonObject registration = hardwareKey.makeRegistrationJson(challenge);

        // now finalise
        Assertions.assertThrows(AssertionError.class,
                () -> WebAuthnEndpointHelper.invokeRegistration("stef", registration, cookieFilter));

        // make sure we did not create any user
        Assertions.assertTrue(userProvider.findByUserName("stef").await().indefinitely().isEmpty());
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
