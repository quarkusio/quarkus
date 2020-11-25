package io.quarkus.it.nat.test.profile;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(RuntimeProfileChangeTest.CustomTestProfile.class)
public class RuntimeProfileChangeTest {
    @Test
    public void failInNativeTestExtension_beforeEach() {
        RestAssured.when()
                .get("/native-config-profile/myConfigValue")
                .then()
                .body(is("bar"));
    }

    public static class CustomTestProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "bar-profile";
        }

    }
}
