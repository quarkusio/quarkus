package io.quarkus.security.webauthn.test;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

/**
 * Same test as WebAuthnManualTest but with custom cookies configured
 */
public class WebAuthnManualCustomCookiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset("quarkus.webauthn.cookie-name=main-cookie\n"
                            + "quarkus.webauthn.challenge-cookie-name=challenge-cookie\n"
                            + "quarkus.webauthn.challenge-username-cookie-name=username-cookie\n"), "application.properties")
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, WebAuthnHardware.class,
                            TestResource.class, ManualResource.class, TestUtil.class));

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
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("stef", cookieFilter);
        WebAuthnHardware hardwareKey = new WebAuthnHardware(url);
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
                .cookie("challenge-cookie", Matchers.is(""))
                .cookie("username-cookie", Matchers.is(""))
                .cookie("main-cookie", Matchers.notNullValue());

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
                .post("/login")
                .then().statusCode(200)
                .body(Matchers.is("OK"))
                .cookie("challenge-cookie", Matchers.is(""))
                .cookie("username-cookie", Matchers.is(""))
                .cookie("main-cookie", Matchers.notNullValue());

        // make sure we bumped the user
        users = userProvider.findByUserName("stef").await().indefinitely();
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
