package io.quarkus.elytron.security.jdbc.it;

import static org.hamcrest.Matchers.containsString;

import org.hamcrest.Matchers;
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
    void permitted() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        // permitted because admin has assigned 'read' permission in 'PermissionIdentityAugmentor'
        RestAssured.given()
                .redirects().follow(false)
                .filter(cookies)
                .when()
                .get("/api/read-permission")
                .then()
                .statusCode(200)
                .body(containsString("withReadPermission"));
    }

    @Test
    void notPermitted() {
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
                .redirects().follow(false)
                .filter(cookies)
                .when()
                .get("/api/read-permission")
                .then()
                .statusCode(403);
    }

    @Test
    void permissionBasedOnSecuredMethodArguments() {
        CookieFilter cookies = new CookieFilter();
        // user 'worker' is assigned 'Workday' permission checker in 'PermissionIdentityAugmentor'
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "worker")
                .formParam("j_password", "worker")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        // not permitted because 'Saturday' is not a workday
        String day = "Saturday";
        RestAssured.given()
                .redirects().follow(false)
                .filter(cookies)
                .when()
                .body(day)
                .get("/api/day-based-permission")
                .then()
                .statusCode(403);

        // permitted because 'Monday' is a workday
        day = "Monday";
        RestAssured.given()
                .redirects().follow(false)
                .filter(cookies)
                .when()
                .body(day)
                .get("/api/day-based-permission")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(day));
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

    @Test
    void testJdbcPermissionChecker() {
        CookieFilter userCookies = new CookieFilter();
        RestAssured
                .given()
                .filter(userCookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "user")
                .formParam("j_password", "user")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        RestAssured.given()
                .header("username", "user")
                .redirects().follow(false)
                .filter(userCookies)
                .when()
                .get("/api/permission-checker")
                .then()
                .statusCode(403);

        CookieFilter adminCookies = new CookieFilter();
        RestAssured
                .given()
                .filter(adminCookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "user")
                .formParam("j_password", "user")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        RestAssured.given()
                .header("username", "admin")
                .redirects().follow(false)
                .filter(adminCookies)
                .when()
                .get("/api/permission-checker")
                .then()
                .statusCode(200)
                .body(containsString("permission-checker"));
    }
}
