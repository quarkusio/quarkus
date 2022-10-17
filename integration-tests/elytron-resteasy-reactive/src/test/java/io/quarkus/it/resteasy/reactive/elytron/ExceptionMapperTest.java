package io.quarkus.it.resteasy.reactive.elytron;

import static io.restassured.RestAssured.given;

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

    public static class ExceptionMapperTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.proactive", "false",
                    "quarkus.http.auth.permission.default.paths", "/*",
                    "quarkus.http.auth.permission.default.policy", "authenticated");
        }
    }

}
