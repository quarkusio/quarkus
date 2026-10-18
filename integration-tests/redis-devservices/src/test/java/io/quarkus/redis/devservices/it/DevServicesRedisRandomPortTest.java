package io.quarkus.redis.devservices.it;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.devservices.it.profiles.DevServicesRandomPortProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServicesRandomPortProfile.class)
public class DevServicesRedisRandomPortTest {

    @BeforeEach
    public void setUp() {
        when().get("/set/anykey/anyvalue").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    @DisplayName("given redis container must communicate with it and return value by key")
    public void shouldReturnAllKeys() {
        when().get("/get/anykey").then()
                .statusCode(200)
                .body(is("anyvalue"));
    }

}
