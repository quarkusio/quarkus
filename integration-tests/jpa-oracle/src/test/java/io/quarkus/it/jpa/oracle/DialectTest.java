package io.quarkus.it.jpa.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test the dialect used by default in Quarkus.
 */
@QuarkusTest
public class DialectTest {

    private static String getExpectedVersion() {
        // When an explicit db-version is provided (e.g. Oracle Test Pilot CI with an external database),
        // the dialect version should match that, not the default.
        String version = System.getProperty("oracle.db-version");
        if (version == null || version.isEmpty()) {
            version = System.getProperty("oracle.default.version");
        }
        assertThat(version)
                .as("oracle.db-version or oracle.default.version system property should be set by maven-surefire-plugin")
                .isNotNull();
        return version;
    }

    /**
     * We want to use a dialect version matching the default DB version as defined in the POM (and forwarded to Hibernate
     * through the datasource extension).
     */
    @Test
    public void version() {
        String version = RestAssured.when().get("/dialect/version").then().extract().body().asString();
        assertThat(version).startsWith(getExpectedVersion());
    }

}
