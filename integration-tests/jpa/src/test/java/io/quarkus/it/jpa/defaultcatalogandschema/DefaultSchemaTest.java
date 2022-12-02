package io.quarkus.it.jpa.defaultcatalogandschema;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DefaultSchemaTest.DefaultSchema1Profile.class)
public class DefaultSchemaTest {
    public static class DefaultSchema1Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.hibernate-orm.database.default-schema", "SCHEMA1",
                    // import.sql doesn't take our custom schema into account.
                    // It should be modified in order to work in this test, but we simply don't need it.
                    "quarkus.hibernate-orm.sql-load-script", "no-file");
        }
    }

    @Test
    public void test() {
        given().queryParam("expectedSchema", "SCHEMA1")
                .when().get("/jpa-test/default-catalog-and-schema/test").then()
                .body(is("OK"))
                .statusCode(200);
    }

}
