package io.quarkus.it.nat.test.profile;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(RuntimeValueChangeTest.CustomTestProfile.class)
public class RuntimeValueChangeTest {
    @Test
    public void failInNativeTestExtension_beforeEach() {
        RestAssured.when()
                .get("/native-config-profile/myConfigValue")
                .then()
                .body(is("RuntimeTimeValueChangeTest"));
    }

    public static class CustomTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("my.config.value", "RuntimeTimeValueChangeTest");
        }

    }
}
