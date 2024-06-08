package io.quarkus.it.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class BuildInfoTestCase {

    @Test
    public void testConfigPropertiesProperlyInjected() {
        String sBuildTimestamp = RestAssured
                .when().get("/build-info")
                .then().extract().response().asString();
        var buildTimestamp = Instant.parse(sBuildTimestamp);
        assertThat(buildTimestamp).isCloseTo(Instant.now(), within(6, ChronoUnit.HOURS));
    }
}
