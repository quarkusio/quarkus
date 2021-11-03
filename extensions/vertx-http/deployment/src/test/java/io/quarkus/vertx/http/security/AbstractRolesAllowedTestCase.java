package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.test.utils.TestIdentityController;
import io.restassured.RestAssured;

public abstract class AbstractRolesAllowedTestCase {

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    @Test
    public void testRolesAllowed() {

        RestAssured
                .given()
                .when()
                .get("/roles1")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/roles1")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/roles1"));

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/roles1")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/roles1"));
    }

    @Test
    public void testRolesAllowedWrongRoles() {

        RestAssured
                .given()
                .when()
                .get("/roles2")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/roles2")
                .then()
                .assertThat()
                .statusCode(403);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/roles2")
                .then()
                .assertThat()
                .statusCode(403);
    }

    @Test
    public void testRolesAllowedCombinedWithPermitAll() {

        RestAssured
                .given()
                .when()
                .get("/permit")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/permit")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/permit"));
    }

    @Test
    public void testRolesAllowedCombinedWithDenyAll() {

        RestAssured
                .given()
                .when()
                .get("/deny")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/deny")
                .then()
                .assertThat()
                .statusCode(403);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/deny")
                .then()
                .assertThat()
                .statusCode(403);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/combined")
                .then()
                .assertThat()
                .statusCode(403);
    }

    @Test
    public void testWildcardMatchingWithSlash() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/wildcard1/a")
                .then()
                .assertThat()
                .statusCode(200);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/wildcard1/a/")
                .then()
                .assertThat()
                .statusCode(200);

        RestAssured
                .given()
                .when()
                .get("/wildcard1/a")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .when()
                .get("/wildcard1/a/")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .when()
                .get("/wildcard3XXX")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    public void testWildcardMatchingWithoutSlash() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/wildcard2/a")
                .then()
                .assertThat()
                .statusCode(200);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/wildcard2")
                .then()
                .assertThat()
                .statusCode(200);

        RestAssured
                .given()
                .when()
                .get("/wildcard2")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .when()
                .get("/wildcard2/a")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testLargeBodyRejected() {

        StringBuilder sb = new StringBuilder("HELLO WORLD");
        for (int i = 0; i < 20; ++i) {
            sb.append(sb);
        }
        for (int i = 0; i < 10; ++i) {
            RestAssured
                    .given()
                    .body(sb.toString())
                    .post("/roles1")
                    .then()
                    .assertThat()
                    .statusCode(401);
        }

    }
}
