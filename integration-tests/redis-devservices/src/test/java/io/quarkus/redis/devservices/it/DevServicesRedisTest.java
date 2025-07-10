package io.quarkus.redis.devservices.it;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.devservices.it.profiles.DevServiceRedis;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.ports.SocketKit;

@QuarkusTest
@TestProfile(DevServiceRedis.class)
public class DevServicesRedisTest {

    @BeforeEach
    public void setUp() {
        when().get("/set/anykey/anyvalue").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    @DisplayName("given quarkus.redis.hosts disabled should start redis testcontainer")
    public void shouldStartRedisContainer() {
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(6379));
    }

    @Test
    @DisplayName("given redis container must communicate with it and return value by key")
    public void shouldReturnAllKeys() {
        when().get("/get/anykey").then()
                .statusCode(200)
                .body(is("anyvalue"));
    }

}
