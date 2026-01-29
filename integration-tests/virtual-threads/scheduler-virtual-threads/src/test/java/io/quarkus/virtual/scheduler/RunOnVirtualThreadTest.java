package io.quarkus.virtual.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestProfile(RunOnVirtualThreadTest.CustomVirtualThreadProfile.class)
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    public static class CustomVirtualThreadProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.virtual-threads.name-prefix", "quarkus-virtual-thread-");
        }
    }

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
