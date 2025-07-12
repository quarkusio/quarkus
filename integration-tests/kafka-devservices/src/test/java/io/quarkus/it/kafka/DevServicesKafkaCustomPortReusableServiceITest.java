package io.quarkus.it.kafka;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.devservices.profiles.DevServicesCustomPortReusableServiceProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.ports.SocketKit;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(DevServicesCustomPortReusableServiceProfile.class)
public class DevServicesKafkaCustomPortReusableServiceITest {

    @Test
    @DisplayName("should start kafka container with the given custom port")
    public void shouldStartKafkaContainer() {
        // We could strengthen this test to make sure the container is the same as seen by other tests, but it's hard since we won't know the order
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(5050));
        RestAssured.when().get("/kafka/port").then().body(Matchers.is("5050"));
    }

}
