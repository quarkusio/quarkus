package io.quarkus.it.keycloak;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.TextPage;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

@QuarkusTest
@QuarkusTestResource(SpiffeKeycloakTestResource.class)
@TestMethodOrder(OrderAnnotation.class)
class SpiffeOidcTest {

    DevServicesContext context;

    @Order(1)
    @Test
    void testOidcClientWithSpiffeAssertion() {
        String token = RestAssured.when().get("/spiffe/client/token")
                .then().statusCode(200)
                .extract().body().asString();

        JsonObject claims = OidcCommonUtils.decodeJwtContent(token);
        assertNotNull(claims);
        assertThat(claims.getString("preferred_username"), equalTo("service-account-quarkus-app-spiffe"));
        assertThat(claims.getString("azp"), equalTo("quarkus-app-spiffe"));
        assertThat(claims.getString("iss"), endsWith("/realms/quarkus-spiffe"));
    }

    @Order(2)
    @Test
    void testCodeFlowWithSpiffeAssertion() throws IOException {
        assertCodeFlowReturnsUser();
    }

    @Order(3)
    @Test
    void testBearerTokenFromSpiffeOidcClient() {
        String token = RestAssured.when().get("/spiffe/client/token")
                .then().statusCode(200)
                .extract().body().asString();

        RestAssured.given()
                .auth().oauth2(token)
                .when().get("/spiffe/bearer")
                .then().statusCode(200)
                .body(equalTo("service-account-quarkus-app-spiffe"));
    }

    @Order(4)
    @Test
    void testSpiffeAuthorizationDenied() {
        try {
            setSpiffeServerMode("PERMISSION_DENIED");
            assertThrows(FailingHttpStatusCodeException.class, () -> assertDynamicCodeFlowReturnsUser("authz-denied"));
        } finally {
            setSpiffeServerMode("HEALTHY");
        }
    }

    @Order(5)
    @Test
    void testSpiffeConnectionFailure() {
        try {
            setSpiffeServerMode("UNAVAILABLE");
            assertThrows(FailingHttpStatusCodeException.class,
                    () -> assertDynamicCodeFlowReturnsUser("unavailable"));
        } finally {
            setSpiffeServerMode("HEALTHY");
        }
    }

    @Order(6)
    @Test
    void testSpiffeAuthorizationDeniedAndRecovery() throws IOException {
        try {
            setSpiffeServerMode("PERMISSION_DENIED");
            assertThrows(FailingHttpStatusCodeException.class, () -> assertDynamicCodeFlowReturnsUser("recovery"));
        } finally {
            setSpiffeServerMode("HEALTHY");
        }
        assertDynamicCodeFlowReturnsUser("recovery");
    }

    private static void assertCodeFlowReturnsUser() throws IOException {
        assertCodeFlowReturnsUser("/spiffe/code-flow");
    }

    private static void assertDynamicCodeFlowReturnsUser(String tenant) throws IOException {
        assertCodeFlowReturnsUser("/spiffe/code-flow?tenant=" + tenant);
    }

    private static void assertCodeFlowReturnsUser(String path) throws IOException {
        try (WebClient webClient = new WebClient()) {
            webClient.setCssErrorHandler(new SilentCssErrorHandler());
            HtmlPage page = webClient.getPage("http://localhost:8081" + path);
            HtmlForm loginForm = page.getForms().get(0);
            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");
            TextPage textPage = loginForm.getButtonByName("login").click();
            assertEquals("alice", textPage.getContent());
        }
    }

    private void setSpiffeServerMode(String mode) {
        String baseUrl = context.devServicesProperties().get("dev-svc.quarkus.spiffe-client.devservices.base-url");
        RestAssured.given().body(mode).post(baseUrl + "/api/admin/mode").then().statusCode(200);
    }
}
