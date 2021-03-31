package io.quarkus.it.resteasy.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class TestSecurityTestCase {

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
                .body(is("testUser"));
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

    static Stream<Arguments> arrayParams() {
        return Stream.of(
                arguments(new int[] { 1, 2 }, new String[] { "hello", "world" }));
    }

}
