package io.quarkus.it.jpa.preferredhibernatetypesoverride;

import io.quarkus.it.jpa.defaultcatalogandschema.DefaultSchemaTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

@QuarkusTest
@TestProfile(OverriddenPreferredTypesResourceIT.OverriddenPreferredTypesSchemaTestProfile.class)
public class OverriddenPreferredTypesResourceIT {
    public static class OverriddenPreferredTypesSchemaTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.hibernate-orm.database.default-schema", "SCHEMA1",
                    "quarkus.hibernate-orm.sql-load-script", "no-file",
                    "quarkus.hibernate-orm.mapping.duration.preferred-jdbc-type", "INTERVAL_SECOND",
                    "quarkus.hibernate-orm.mapping.instant.preferred-jdbc-type", "INSTANT",
                    "quarkus.hibernate-orm.mapping.boolean.preferred-jdbc-type", "BIT",
                    "quarkus.hibernate-orm.mapping.uuid.preferred-jdbc-type", "CHAR"
            );
        }
    }

    @Test
    void shouldSaveEntityWithOverriddenTypes() {
        given().queryParam("expectedSchema", "SCHEMA1")
                .when().get("/jpa-test/overridden-preferred-types/test").then()
                .body(is("OK"))
                .statusCode(200);
    }
}
