package io.quarkus.security.webauthn.test;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnRunTimeConfig;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.specification.RequestSpecification;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.core.json.JsonObject;

public class WebAuthnAndBasicAuthnTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebAuthnManualTestUserProvider.class, WebAuthnTestUserProvider.class, WebAuthnHardware.class,
                            TestResource.class, ManualResource.class, TestUtil.class, TestIdentityProvider.class,
                            MultipleAuthMechResource.class, TestIdentityController.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.basic=true\n" +
                            "quarkus.http.auth.proactive=false\n"), "application.properties"));

    @Inject
    WebAuthnUserProvider userProvider;

    @TestHTTPResource
    URL url;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("basic", "basic", "basic");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void test() throws Exception {

        Assertions.assertTrue(userProvider.findByUserName("stev").await().indefinitely().isEmpty());
        CookieFilter cookieFilter = new CookieFilter();
        String challenge = WebAuthnEndpointHelper.obtainRegistrationChallenge("stev", cookieFilter);
        WebAuthnHardware hardwareKey = new WebAuthnHardware(url);
        JsonObject registration = hardwareKey.makeRegistrationJson(challenge);

        // now finalise
        RequestSpecification request = RestAssured
                .given()
                .filter(cookieFilter);
        WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registration);
        var config = new SmallRyeConfigBuilder()
                .withMapping(WebAuthnRunTimeConfig.class)
                .build()
                .getConfigMapping(WebAuthnRunTimeConfig.class);
        request
                .queryParam("username", "stev")
                .post("/register")
                .then().statusCode(200)
                .body(Matchers.is("OK"))
                .cookie(config.challengeCookieName(), Matchers.is(""))
                .cookie(config.challengeUsernameCookieName(), Matchers.is(""))
                .cookie("quarkus-credential", Matchers.notNullValue());

        // make sure we stored the user
        List<WebAuthnCredentialRecord> users = userProvider.findByUserName("stev").await().indefinitely();
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.get(0).getUserName().equals("stev"));
        Assertions.assertEquals(1, users.get(0).getCounter());

        // make sure our login cookie works
        checkLoggedIn(cookieFilter);

        // check that when an endpoint is annotated with @Basic, web auth won't work
        RestAssured.given().filter(cookieFilter).post("/multiple-auth-mech/basic").then().statusCode(401);
        // check that when an endpoint is annotated with @Basic, basic auth works
        RestAssured.given().auth().preemptive().basic("basic", "basic").post("/multiple-auth-mech/basic").then().statusCode(200)
                .body(Matchers.is("basic"));

        // check that when an endpoint is annotated with @WebAuthn, webuauth works
        RestAssured.given().filter(cookieFilter).post("/multiple-auth-mech/webauth").then().statusCode(200)
                .body(Matchers.is("webauth"));
        // check that when an endpoint is annotated with @WebAuthn, basic auth won't work
        RestAssured.given().auth().preemptive().basic("basic", "basic").post("/multiple-auth-mech/webauth").then()
                .statusCode(302);
    }

    private void checkLoggedIn(CookieFilter cookieFilter) {
        RestAssured
                .given()
                .filter(cookieFilter)
                .get("/secure")
                .then()
                .statusCode(200)
                .body(Matchers.is("stev: [admin]"));
    }
}
