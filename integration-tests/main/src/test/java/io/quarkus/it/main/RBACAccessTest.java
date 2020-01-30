package io.quarkus.it.main;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@QuarkusTest
public class RBACAccessTest {

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        String path = "/rbac-secured/forTesterOnly";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("forTesterOnly"));
    }

    @Test
    public void shouldRestrictAccessToSpecificRoleAndMethodParameterAnnotationsShouldntAffectAnything() {
        String path = "/rbac-secured/forTesterOnlyWithMethodParamAnnotations";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("forTesterOnlyWithMethodParamAnnotations"));
    }

    @Test
    public void shouldFailToAccessForbidden() {
        assertForAnonymous("/rbac-secured/denied", 401, Optional.empty());
        assertForUsers("/rbac-secured/denied", 403, Optional.empty());
    }

    @Test
    public void shouldAccessAllowed() {
        assertForAnonymous("/rbac-secured/permitted", 200, Optional.of("permitted"));
        assertForUsers("/rbac-secured/permitted", 200, Optional.of("permitted"));
    }

    @Test
    public void shouldRestrictAuthenticated() {
        assertForAnonymous("/rbac-secured/authenticated", 401, Optional.empty());
        assertForUsers("/rbac-secured/authenticated", 200, Optional.of("authenticated"));
    }

    @Test
    public void shouldRestrictAccessToSpecificRoleOnBean() {
        String path = "/rbac-secured/callingTesterOnly";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("callingTesterOnly"));
    }

    @Test
    public void shouldFailToAccessForbiddenOnBean() {
        assertForAnonymous("/rbac-secured/callingDenied", 401, Optional.empty());
        assertForUsers("/rbac-secured/callingDenied", 403, Optional.empty());
    }

    @Test
    public void shouldAccessAllowedOnBean() {
        assertForAnonymous("/rbac-secured/callingPermitted", 200, Optional.of("callingPermitted"));
        assertForUsers("/rbac-secured/callingPermitted", 200, Optional.of("callingPermitted"));
    }

    @Test
    public void shouldRestrictAuthenticatedOnBean() {
        assertForAnonymous("/rbac-secured/callingAuthenticated", 401, Optional.empty());
        assertForUsers("/rbac-secured/callingAuthenticated", 200, Optional.of("callingAuthenticated"));
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
