package io.quarkus.it.hibernate.search.orm.elasticsearch;

import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
@TestProfile(MappingExtensionTest.Profile.class)
public class MappingExtensionTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // No mapping configurers so the one from the extension should be in effect
            return Map.of("quarkus.hibernate-search-orm.mapping.configurer", "");
        }
    }

    @Test
    public void testMapping() {
        RestAssured.when().put("/test/mapping/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/mapping/mapping-extension").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
