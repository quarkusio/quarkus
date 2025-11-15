package io.quarkus.it.kafka;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.devservices.profiles.DevServicesNonUniquePortProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.ports.SocketKit;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(DevServicesNonUniquePortProfile.class)
public class DevServicesKafkaNonUniquePortITest {

    @Test
    @DisplayName("should start kafka container with the given custom port")
    public void shouldStartKafkaContainer() {
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(5050));
        RestAssured.when().get("/kafka/port").then().body(Matchers.is("5050"));
    }

    @Test
    public void shouldBeCorrectImage() {
        RestAssured.when().get("/kafka/image").then().body(Matchers.is("redpanda"));
    }

}
