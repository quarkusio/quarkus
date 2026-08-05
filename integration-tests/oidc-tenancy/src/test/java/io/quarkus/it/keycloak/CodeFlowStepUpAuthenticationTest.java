package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.BearerTokenAuthorizationTest.createWebClient;
import static io.quarkus.it.keycloak.BearerTokenAuthorizationTest.getSessionCookie;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.Tenant;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@TestProfile(BearerTokenStepUpAuthenticationTest.StepUpAuthTestProfile.class)
@QuarkusTestResource(KeycloakRealmResourceManager.class)
@QuarkusTest
public class CodeFlowStepUpAuthenticationTest {

    @Test
    public void testLoginRedirectContainsAcrValues() {
        // anonymous request to the endpoint requiring an authentication level,
        // the authorization request must ask for it with the 'acr_values' parameter
        String location = RestAssured.given().redirects().follow(false)
                .when().get("/code-flow-step-up-auth/acr-3")
                .then().statusCode(302).extract().header("location");
        assertThat(location, containsString("/realms/quarkus-webapp/protocol/openid-connect/auth"));
        assertThat(location, containsString("acr_values=3"));
        assertThat(location, not(containsString("max_age")));
    }

    @Test
    public void testLoginRedirectContainsMultipleAcrValuesAndMaxAge() {
        String location = RestAssured.given().redirects().follow(false)
                .when().get("/code-flow-step-up-auth/acr-multiple-max-age")
                .then().statusCode(302).extract().header("location");
        assertThat(location, containsString("/realms/quarkus-webapp/protocol/openid-connect/auth"));
        // the required acr values are passed as a space separated list, in no particular order
        assertThat(location, anyOf(containsString("acr_values=2+3"), containsString("acr_values=3+2")));
        assertThat(location, containsString("max_age=300"));
    }

    @Test
    public void testCodeFlowSatisfiesRequiredAcr() throws IOException {
        try (WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-step-up-auth/acr-1");
            // Keycloak login page, reached with the authorization request requiring acr '1'
            assertEquals("Sign in to quarkus-webapp", page.getTitleText());
            assertThat(page.getUrl().toString(), containsString("acr_values=1"));

            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            // a fresh login satisfies acr '1', the ID token carries the claim
            assertEquals("granted:1", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "step-up-auth-web-app"));

            // the existing session satisfies the required level, no new challenge
            page = webClient.getPage("http://localhost:8081/code-flow-step-up-auth/acr-1");
            assertEquals("granted:1", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testStepUpFromExistingSession() throws IOException {
        try (WebClient webClient = createWebClient()) {
            // establish a session with the standard acr level '1'
            HtmlPage page = webClient.getPage("http://localhost:8081/code-flow-step-up-auth/acr-1");
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            page = loginForm.getButtonByName("login").click();
            assertEquals("granted:1", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "step-up-auth-web-app"));

            // the session does not satisfy acr '3', the user must be redirected
            // to the provider to re-authenticate with the required acr, not get a 401
            webClient.getOptions().setRedirectEnabled(false);
            WebResponse webResponse = webClient.loadWebResponse(
                    new WebRequest(URI.create("http://localhost:8081/code-flow-step-up-auth/acr-3").toURL()));
            assertEquals(302, webResponse.getStatusCode());
            String location = webResponse.getResponseHeaderValue("location");
            assertThat(location, containsString("/realms/quarkus-webapp/protocol/openid-connect/auth"));
            assertThat(location, containsString("acr_values=3"));
            // the session cookie is removed so that a new login can replace the session
            assertNull(getSessionCookie(webClient, "step-up-auth-web-app"));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Tenant("step-up-auth-web-app")
    @Path("/code-flow-step-up-auth")
    public static class CodeFlowStepUpResource {

        @Inject
        @IdToken
        JsonWebToken idToken;

        @GET
        @Path("acr-1")
        @AuthenticationContext("1")
        public String acr1() {
            return "granted:" + idToken.getClaim("acr");
        }

        @GET
        @Path("acr-3")
        @AuthenticationContext("3")
        public String acr3() {
            return "granted";
        }

        @GET
        @Path("acr-multiple-max-age")
        @AuthenticationContext(value = { "2", "3" }, maxAge = "PT300s")
        public String acrMultipleMaxAge() {
            return "granted";
        }
    }
}
