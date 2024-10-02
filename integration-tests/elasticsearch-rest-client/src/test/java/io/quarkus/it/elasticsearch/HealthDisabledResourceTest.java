package io.quarkus.it.elasticsearch;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.elasticsearch.profile.HealthDisabledTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(HealthDisabledTestProfile.class)
public class HealthDisabledResourceTest {
    @Test
    public void testHealth() {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks", empty());
    }
}
