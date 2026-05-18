package io.quarkus.it.jpa.db2;

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
        String version = System.getProperty("db2.version");
        assertThat(version)
                .as("db2.version system property should be set by maven-surefire-plugin")
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
