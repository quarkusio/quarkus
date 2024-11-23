package io.quarkus.it.spring.web;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class SecurityTest {

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        String path = "/api/securedMethod";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("accessibleForAdminOnly"));
    }

    @Test
    public void testAllowedForAdminOrViewer() {
        String path = "/api/allowedForUserOrViewer";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("aurea", "auri"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 200,
                Optional.of("allowedForUserOrViewer"));
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("george", "geo"), path, 200,
                Optional.of("allowedForUserOrViewer"));
    }

    @Test
    public void testWithAlwaysFalseChecker() {
        String path = "/api/withAlwaysFalseChecker";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("george", "geo"), path, 403, Optional.empty());
    }

    @Test
    public void testPreAuthorizeOnController() {
        String path = "/api/preAuthorizeOnController";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 200,
                Optional.of("preAuthorizeOnController"));
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("aurea", "auri"), path, 200,
                Optional.of("preAuthorizeOnController"));
    }

    @Test
    public void preAuthorizeOnControllerWithArgs() {
        String path = "/api/preAuthorizeOnControllerWithArgs/";
        assertForAnonymous(path + "correct-name", 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path + "wrong-name", 403,
                Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("aurea", "auri"), path + "correct-name", 200,
                Optional.of("Hello correct-name!"));
    }

    @Test
    public void shouldAccessAllowed() {
        assertForAnonymous("/api/accessibleForAllMethod", 200, Optional.of("accessibleForAll"));
        assertForUsers("/api/accessibleForAllMethod", 200, Optional.of("accessibleForAll"));
    }

    @Test
    public void shouldRestrictAccessOnClass() {
        assertForAnonymous("/api/restrictedOnClass", 401, Optional.empty());
        assertForUsers("/api/restrictedOnClass", 200, Optional.of("restrictedOnClass"));
    }

    @Test
    public void shouldFailToAccessRestrictedOnClass() {
        assertForAnonymous("/api/restrictedOnMethod", 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), "/api/restrictedOnMethod", 403,
                Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), "/api/restrictedOnMethod", 200,
                Optional.of("restrictedOnMethod"));
    }

    private void assertForAnonymous(String path, int status, Optional<String> content) {
        assertStatusAndContent(RestAssured.given(), path, status, content);
    }

    private void assertForUsers(String path, int status, Optional<String> content) {
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, status, content);
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, status, content);
    }

    private void assertStatusAndContent(RequestSpecification request, String path, int status, Optional<String> content) {
        ValidatableResponse validatableResponse = request.when().get(path)
                .then()
                .statusCode(status);
        content.ifPresent(text -> validatableResponse.body(Matchers.equalTo(text)));
    }
}
