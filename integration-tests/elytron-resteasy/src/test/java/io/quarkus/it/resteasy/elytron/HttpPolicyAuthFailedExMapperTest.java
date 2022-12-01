package io.quarkus.it.resteasy.elytron;

import static io.quarkus.it.resteasy.elytron.AuthFailedExceptionMapper.EXPECTED_RESPONSE;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(HttpPolicyAuthFailedExMapperTest.CustomTestProfile.class)
public class HttpPolicyAuthFailedExMapperTest {

    @Test
    public void testAuthFailedExceptionMapper() {
        RestAssured
                .given()
                .auth().basic("unknown-user", "unknown-pwd")
                .contentType(ContentType.TEXT)
                .get("/")
                .then()
                .statusCode(401)
                .body(Matchers.equalTo(EXPECTED_RESPONSE));
    }

    public static class CustomTestProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "auth-failed-ex-mapper";
        }

    }
}
