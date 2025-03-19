package io.quarkus.it.keycloak;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
public class ManagementResourceTest {

    @Test
    void testTokenWithUserGroup() {

        var token = Jwt.upn("jdoe@quarkus.io")
                .groups("USER")
                .claim(Claims.birthdate.name(), "2001-07-13")
                .sign();

        given()
                .header(new Header("Authorization", "Bearer " + token))
                .when().get("/management/only-user")
                .then()
                .statusCode(200)
                .body(is("User Panel ::: Management"));
    }

    @Test
    void testTokenWithWrongGroup() {
        var token = Jwt.upn("jdoe@quarkus.io")
                .groups("INVALID_GROUP")
                .claim(Claims.birthdate.name(), "2001-07-13")
                .sign();

        given()
                .header(new Header("Authorization", "Bearer " + token))
                .when().get("/management/only-user")
                .then()
                .statusCode(403);
    }

}
