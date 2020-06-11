package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * Tests that QuarkusTestProfile works as expected
 */
@QuarkusTest
@TestProfile(GreetingProfileTestCase.MyProfile.class)
public class GreetingProfileTestCase {

    @Test
    public void included() {
        RestAssured.when()
                .get("/greeting/Stu")
                .then()
                .statusCode(200)
                .body(is("Bonjour Stu"));
    }

    @Test
    public void testPortTakesEffect() {
        Assertions.assertEquals(7777, RestAssured.port);
    }

    public static class MyProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("quarkus.http.test-port", "7777");
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Collections.singleton(BonjourService.class);
        }
    }
}
