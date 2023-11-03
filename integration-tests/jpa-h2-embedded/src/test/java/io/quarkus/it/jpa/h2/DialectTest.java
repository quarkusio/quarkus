package io.quarkus.it.jpa.h2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test the dialect used by default in Quarkus.
 */
@QuarkusTest
public class DialectTest {

    /**
     * This is important to avoid https://github.com/quarkusio/quarkus/issues/1886
     */
    @Test
    public void version() {
        String version = RestAssured.when().get("/dialect/version").then().extract().body().asString();
        assertThat(version).startsWith(DialectVersions.Defaults.H2);
    }

}
