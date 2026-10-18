package io.quarkus.it.jpa.postgresql;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/* Exercises OverrideJdbcUrlBuildTimeConfigSource, which cannot coexist with dev services. */
@TestProfile(ConfigOverrideTest.Profile.class)
@QuarkusTest
public class ConfigOverrideTest {

    @Test
    public void base() {
        RestAssured.when().get("/jpa/testfunctionality/base").then().body(is("OK"));
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // Disable dev services so we know we're connecting to the right database
            return Map.of(
                    "quarkus.devservices.enabled", "false",
                    // These are no longer the default when not using dev services
                    "quarkus.hibernate-orm.schema-management.strategy", "drop-and-create",
                    "quarkus.hibernate-orm.\"other\".schema-management.strategy", "drop-and-create");
        }

        @Override
        public String getConfigProfile() {
            return "someotherprofile";
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return Collections.singletonList(new TestResourceEntry(PostgresTestResourceLifecycleManager.class));
        }
    }
}
