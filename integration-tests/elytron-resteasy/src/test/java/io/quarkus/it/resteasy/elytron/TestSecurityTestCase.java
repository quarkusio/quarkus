package io.quarkus.it.resteasy.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.ForbiddenException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.AttributeType;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;

@QuarkusTest
class TestSecurityTestCase {

    @Inject
    BeanSecuredWithPermissions beanSecuredWithPermissions;

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testGet() {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(200)
                .body(is("secure"));
    }

    @TestSecurity
    @ParameterizedTest
    @ValueSource(ints = 1) //https://github.com/quarkusio/quarkus/issues/12413
    void testGetWithSecEnabled(int ignore) {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(401);
    }

    @TestSecurity
    @ParameterizedTest
    @MethodSource("arrayParams") //https://github.com/quarkusio/quarkus/issues/12413
    void testGetUnAuthorized(int[] ignoredPrimitives, String[] ignored) {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "wrong")
    void testGetWithTestUser() {
        given()
                .when()
                .get("/secure")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "wrong")
    void testGetWithTestUserWrongRole() {
        given()
                .when()
                .get("/user")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user")
    void testTestUserCorrectRole() {
        given()
                .when()
                .get("/user")
                .then()
                .statusCode(200)
                .body(is("testUser:testUser:testUser"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user", attributes = { @SecurityAttribute(key = "foo", value = "bar") })
    void testAttributes() {
        given()
                .when()
                .get("/attributes")
                .then()
                .statusCode(200)
                .body(is("foo=bar"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user", attributes = {
            @SecurityAttribute(key = "foo", value = "9223372036854775807", type = AttributeType.LONG) })
    void testLongAttributes() {
        given()
                .when()
                .get("/attributes")
                .then()
                .statusCode(200)
                .body(is("foo=" + Long.MAX_VALUE));
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user", attributes = {
            @SecurityAttribute(key = "foo", value = "[\"A\",\"B\",\"C\"]", type = AttributeType.JSON_ARRAY) })
    void testJsonArrayAttributes() {
        given()
                .when()
                .get("/attributes")
                .then()
                .statusCode(200)
                .body(is("foo=[\"A\",\"B\",\"C\"]"));
    }

    @Test
    @TestSecurity(user = "testUser", roles = "user", attributes = {
            @SecurityAttribute(key = "foo", value = "\"A\",\"B\",\"C\"", type = AttributeType.STRING_SET) })
    void testStringSetAttributes() {
        given()
                .when()
                .get("/attributes")
                .then()
                .statusCode(200)
                .body(startsWith("foo=["))
                .and()
                .body(endsWith("]"))
                .and()
                .body(containsString("\"A\""))
                .and()
                .body(containsString("\"B\""))
                .and()
                .body(containsString("\"C\""));
    }

    @Test
    @TestSecurity(user = "testUser", permissions = "wrong-permissions")
    void testInsufficientPermissions() {
        Assertions.assertThrows(ForbiddenException.class, beanSecuredWithPermissions::getDetail);
    }

    @Test
    @TestSecurity(user = "testUser", permissions = { "see:all", "create:all" })
    void testPermissionsAndActions_AllAction() {
        Assertions.assertEquals("detail", beanSecuredWithPermissions.getDetail());
        Assertions.assertEquals("created", beanSecuredWithPermissions.create());
        // fails as requires "modify" permission
        Assertions.assertThrows(ForbiddenException.class, beanSecuredWithPermissions::modify);
    }

    @Test
    @TestSecurity(user = "testUser", permissions = "see:detail")
    void testPermissionsAndActions_DetailAction() { // check both actions are added to possessed permission
        Assertions.assertEquals("detail", beanSecuredWithPermissions.getDetail());
        // fails as missing "create:all"
        Assertions.assertThrows(ForbiddenException.class, beanSecuredWithPermissions::create);
        // fails as requires "modify" permission
        Assertions.assertThrows(ForbiddenException.class, beanSecuredWithPermissions::modify);
    }

    @Test
    @TestSecurity(user = "testUser", permissions = "modify")
    void testPermissionsOnly() {
        Assertions.assertEquals("modified", beanSecuredWithPermissions.modify());
        // fails as requires "see:all" or "see:detail"
        Assertions.assertThrows(ForbiddenException.class, beanSecuredWithPermissions::getDetail);
    }

    static Stream<Arguments> arrayParams() {
        return Stream.of(
                arguments(new int[] { 1, 2 }, new String[] { "hello", "world" }));
    }

    @Test
    public void testPermissionChecker_anonymousUser() {
        // user is not authenticated and access should not be granted by the permission checker
        RestAssured.get("/test-security-permission-checker").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "authenticated-user")
    public void testPermissionChecker_authenticatedUser() {
        // user is authenticated, but access should not be granted by the permission checker
        RestAssured.get("/test-security-permission-checker").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "meat loaf")
    public void testPermissionChecker_authorizedUser() {
        // user is authenticated and access should be granted by the permission checker
        RestAssured.get("/test-security-permission-checker").then().statusCode(200)
                .body(Matchers.is("meat loaf:meat loaf:meat loaf"));
    }
}
