package io.quarkus.virtual.security.webauthn;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.vertx.core.json.JsonObject;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Inject
    WebAuthnUserProvider userProvider;

    @TestHTTPResource
    URL url;

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
        WebAuthnHardware hardwareKey = new WebAuthnHardware(url);
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("stef", cookieFilter);
        JsonObject registration = hardwareKey.makeRegistrationJson(challenge);

        // now finalise
        WebAuthnEndpointHelper.invokeRegistration("stef", registration, cookieFilter);

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
        WebAuthnEndpointHelper.invokeLogin(login, cookieFilter);

        // make sure we bumped the user
        users = userProvider.findByUserName("stef").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stef"));
        Assertions.assertEquals(2, users.get(0).getCounter());

        // make sure our login cookie still works
        checkLoggedIn(cookieFilter);
    }

    public static void checkLoggedIn(CookieFilter cookieFilter) {
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
