package io.quarkus.elytron.security.jdbc.it;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

@QuarkusTest
class ElytronSecurityJdbcTest {

    @Test
    void anonymous() {
        RestAssured.given()
                .when()
                .get("/api/anonymous")
                .then()
                .statusCode(200)
                .body(containsString("anonymous"));
    }

    @Test
    void authenticated() {
        CookieFilter cookies = new CookieFilter();
        RestAssured.given()
                .redirects().follow(false)
                .filter(cookies)
                .when()
                .get("/api/authenticated")
                .then()
                .statusCode(302);

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "user")
                .formParam("j_password", "user")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        RestAssured.given()
                .redirects().follow(false)
                .filter(cookies)
                .when()
                .get("/api/authenticated")
                .then()
                .statusCode(200)
                .body(containsString("authenticated"));
    }

    @Test
    void authenticated_not_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/api/authenticated")
                .then()
                .statusCode(302);
    }

    @Test
    void forbidden() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "user")
                .formParam("j_password", "user")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        RestAssured.given()
                .filter(cookies)
                .when()
                .get("/api/forbidden")
                .then()
                .statusCode(403);
    }

    @Test
    void forbidden_not_authenticated() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/api/forbidden")
                .then()
                .statusCode(302);
    }

}
