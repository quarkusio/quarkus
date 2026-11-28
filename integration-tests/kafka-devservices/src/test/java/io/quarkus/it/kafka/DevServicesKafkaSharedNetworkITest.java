package io.quarkus.it.kafka;

import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.devservices.profiles.DevServicesSharedNetworkProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(DevServicesSharedNetworkProfile.class)
public class DevServicesKafkaSharedNetworkITest {

    @Test
    @DisplayName("should give the network alias for containers and the published port for the host")
    public void shouldUseBothListeners() {
        String bootstrapServers = ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class);
        assertThat(bootstrapServers, Matchers.matchesPattern("BROKER://kafka-\\w+:9093,PLAINTEXT://.+:\\d+"));
    }

    @Test
    @DisplayName("should be reachable from the host")
    public void shouldBeReachableFromTheHost() {
        RestAssured.when().get("/kafka/partitions/test").then().body(Matchers.is("2"));
    }

}
