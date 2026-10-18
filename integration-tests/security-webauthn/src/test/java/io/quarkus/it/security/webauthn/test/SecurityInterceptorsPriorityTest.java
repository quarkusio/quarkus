package io.quarkus.it.security.webauthn.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.it.security.webauthn.CustomInterceptor;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.cookie.CookieFilter;
import io.vertx.core.json.JsonObject;

@QuarkusTest
class SecurityInterceptorsPriorityTest {

    @TestHTTPResource
    URL url;

    @Test
    void testSecurityChecksRunsBeforeTransactions() {
        Filter cookieFilter = new CookieFilter();
        registerUser("Roman", cookieFilter);

        CustomInterceptor.securityCheckRun = false;
        CustomInterceptor.sessionNotStarted = false;

        long id = Long.parseLong(RestAssured.given()
                .filter(cookieFilter)
                .pathParam("name", "Roman")
                .get("/api/users/{name}")
                .then()
                .statusCode(200)
                .extract().asString());

        assertTrue(CustomInterceptor.securityCheckRun);
        assertTrue(CustomInterceptor.sessionNotStarted);

        // check we received response from database
        assertTrue(id >= 1);
    }

    private void registerUser(String username, Filter cookieFilter) {
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge(username, cookieFilter);
        WebAuthnHardware token = new WebAuthnHardware(url);
        JsonObject registrationJson = token.makeRegistrationJson(challenge);
        WebAuthnEndpointHelper.invokeRegistration(username, registrationJson, cookieFilter);
    }

}
