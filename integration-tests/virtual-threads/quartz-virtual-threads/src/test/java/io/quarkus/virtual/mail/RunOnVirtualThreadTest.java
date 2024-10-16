package io.quarkus.virtual.mail;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Test
    void testScheduledMethods() {
        Awaitility.await()
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var list = RestAssured.get().then()
                            .assertThat().statusCode(200)
                            .extract().as(new TypeRef<List<String>>() {
                            });
                    Assertions.assertTrue(list.size() > 3);
                });
    }

    @Test
    void testScheduledMethodsUsingApi() {
        Awaitility.await()
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var list = RestAssured.get("/programmatic").then()
                            .assertThat().statusCode(200)
                            .extract().as(new TypeRef<List<String>>() {
                            });
                    Assertions.assertTrue(list.size() > 3);
                });
    }

}
