package io.quarkus.it.jpa.h2;

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
        String actualVersion = System.getProperty("h2.version");
        assertThat(actualVersion)
                .as("h2.version system property should be set by maven-surefire-plugin")
                .isNotNull();
        return actualVersion;
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

    /**
     * This is important to avoid https://github.com/quarkusio/quarkus/issues/1886
     */
    @Test
    public void actualDbVersion() {
        String version = RestAssured.when().get("/dialect/actual-db-version").then().extract().body().asString();
        // Can't use "equal" as the returned string includes trailing information (build date, ...)
        assertThat(version).startsWith(getExpectedVersion());
    }

}
