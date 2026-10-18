package io.quarkus.it.nat.test.profile;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(RuntimeValueChangeFromTestResourcesTest.CustomTestProfile.class)
public class RuntimeValueChangeFromTestResourcesTest {

    private static final String EXPECTED_VALUE = "RuntimeTimeValueChangeFromTestResources";

    @Test
    public void failInNativeTestExtension_beforeEach() {
        RestAssured.when()
                .get("/native-config-profile/myConfigValue")
                .then()
                .body(is(EXPECTED_VALUE));
    }

    public static class CustomTestProfile implements QuarkusTestProfile {
        @Override
        public List<TestResourceEntry> testResources() {
            return Collections.singletonList(new TestResourceEntry(DummyTestResource.class));
        }
    }

    /**
     * This only used to ensure that the TestResource has been handled correctly by the QuarkusTestExtension
     */
    public static class DummyTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.singletonMap("my.config.value", EXPECTED_VALUE);
        }

        @Override
        public void stop() {
            // do nothing
        }
    }
}
