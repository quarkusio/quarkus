package io.quarkus.it.undertow.elytron;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ServletSecurityAnnotationPermissionsTestCase extends HttpsSetup {

    @Test
    void testSecuredServletWithWrongAuth() {
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/foo/annotation-secure")
                .then()
                .statusCode(403);
    }

    @Test
    void testSecuredServletWithNoAuth() {
        given()
                .when()
                .get("/foo/annotation-secure")
                .then()
                .statusCode(401);
    }

    @Test
    void testSecuredServletWithAuth() {
        given()
                .auth()
                .basic("mary", "mary")
                .when()
                .get("/foo/annotation-secure")
                .then()
                .statusCode(200);
    }

    @Test
    void testEmptyRolesPermit() {
        given()
                .when()
                .put("/foo/annotation-secure")
                .then()
                .statusCode(200);
    }

    @Test
    void testEmptyRolesDeny() {
        given()
                .when()
                .delete("/foo/annotation-secure")
                .then()
                .statusCode(401);
    }

    @Test
    void testPostDeniedWrongRole() {
        given()
                .auth()
                .basic("mary", "mary")
                .when()
                .post("/foo/annotation-secure")
                .then()
                .statusCode(403);
    }

    @Test
    void testPostSucess() {
        given()
                .when()
                .body("tmp")
                .auth()
                .basic("poul", "poul")
                .post("/foo/annotation-secure")
                .then()
                .statusCode(200);
    }
}
