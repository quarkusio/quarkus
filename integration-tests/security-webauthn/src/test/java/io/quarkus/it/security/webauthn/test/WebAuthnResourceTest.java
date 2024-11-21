package io.quarkus.it.security.webauthn.test;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class WebAuthnResourceTest {

    enum User {
        USER,
        ADMIN;
    }

    enum Endpoint {
        DEFAULT,
        MANUAL;
    }

    @TestHTTPResource
    URL url;

    @Test
    public void testWebAuthnUser() {
        testWebAuthn("FroMage", User.USER, Endpoint.DEFAULT);
        testWebAuthn("scooby", User.USER, Endpoint.MANUAL);
    }

    @Test
    public void testWebAuthnAdmin() {
        testWebAuthn("admin", User.ADMIN, Endpoint.DEFAULT);
    }

    private void testWebAuthn(String userName, User user, Endpoint endpoint) {
        Filter cookieFilter = new RenardeCookieFilter();
        WebAuthnHardware token = new WebAuthnHardware(url);

        verifyLoggedOut(cookieFilter);

        // two-step registration
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge(userName, cookieFilter);
        JsonObject registrationJson = token.makeRegistrationJson(challenge);
        if (endpoint == Endpoint.DEFAULT)
            WebAuthnEndpointHelper.invokeRegistration(userName, registrationJson, cookieFilter);
        else {
            invokeCustomEndpoint("/register", cookieFilter, request -> {
                WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registrationJson);
                request.formParam("userName", userName);
            });
        }

        // verify that we can access logged-in endpoints
        verifyLoggedIn(cookieFilter, userName, user);

        // logout
        WebAuthnEndpointHelper.invokeLogout(cookieFilter);

        verifyLoggedOut(cookieFilter);

        // two-step login
        challenge = WebAuthnEndpointHelper.obtainLoginChallenge(userName, cookieFilter);
        JsonObject loginJson = token.makeLoginJson(challenge);
        if (endpoint == Endpoint.DEFAULT)
            WebAuthnEndpointHelper.invokeLogin(loginJson, cookieFilter);
        else {
            invokeCustomEndpoint("/login", cookieFilter, request -> {
                WebAuthnEndpointHelper.addWebAuthnLoginFormParameters(request, loginJson);
                request.formParam("userName", userName);
            });
        }

        // verify that we can access logged-in endpoints
        verifyLoggedIn(cookieFilter, userName, user);

        // logout
        WebAuthnEndpointHelper.invokeLogout(cookieFilter);

        verifyLoggedOut(cookieFilter);
    }

    private void invokeCustomEndpoint(String uri, Filter cookieFilter, Consumer<RequestSpecification> requestCustomiser) {
        RequestSpecification request = given()
                .when();
        requestCustomiser.accept(request);
        request
                .filter(cookieFilter)
                .redirects().follow(false)
                .log().ifValidationFails()
                .post(uri)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .cookie(WebAuthnEndpointHelper.getChallengeCookie(), Matchers.is(""))
                .cookie(WebAuthnEndpointHelper.getChallengeUsernameCookie(), Matchers.is(""))
                .cookie(WebAuthnEndpointHelper.getMainCookie(), Matchers.notNullValue());
    }

    private void verifyLoggedIn(Filter cookieFilter, String userName, User user) {
        // public API still good
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

        // user API accessible
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

        // admin API?
        if (user == User.ADMIN) {
            RestAssured.given().filter(cookieFilter)
                    .when()
                    .get("/api/admin")
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("admin"));
        } else {
            RestAssured.given().filter(cookieFilter)
                    .when()
                    .get("/api/admin")
                    .then()
                    .statusCode(403);
        }
    }

    private void verifyLoggedOut(Filter cookieFilter) {
        // public API still good
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is("<not logged in>"));

        // user API not accessible
        RestAssured.given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(302)
                .header("Location", Matchers.is("http://localhost:8081/"));

        // admin API not accessible
        RestAssured.given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .when()
                .get("/api/admin")
                .then()
                .statusCode(302)
                .header("Location", Matchers.is("http://localhost:8081/"));
    }
}