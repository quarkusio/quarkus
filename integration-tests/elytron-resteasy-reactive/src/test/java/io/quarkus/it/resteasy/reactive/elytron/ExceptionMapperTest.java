package io.quarkus.it.resteasy.reactive.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(value = ExceptionMapperTest.ExceptionMapperTestProfile.class)
@QuarkusTest
public class ExceptionMapperTest {

    @Test
    public void testCustomExceptionMapper() {
        given()
                .when()
                .auth().preemptive().basic("unknownUsername", "unknownPassword")
                .get("/fruit/all-with-security")
                .then()
                .statusCode(401)
                .body(Matchers.equalTo("customized"));
    }

    @Test
    void testCustomPermission() {
        // test HTTP policy granting permissions with disabled proactive auth
        given()
                .auth().preemptive().basic("mary", Users.password("mary"))
                .when()
                .get("/manager-permission")
                .then()
                .statusCode(200)
                .body(is("mary"));
        given()
                .auth().preemptive().basic("john", Users.password("john"))
                .when()
                .get("/manager-permission")
                .then()
                .statusCode(403);
    }

    public static class ExceptionMapperTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.proactive", "false",
                    "quarkus.http.auth.permission.default.paths", "/*",
                    "quarkus.http.auth.permission.default.policy", "authenticated");
        }
    }

}
