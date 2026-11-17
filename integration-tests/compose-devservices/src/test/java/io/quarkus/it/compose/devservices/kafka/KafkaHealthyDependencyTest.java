package io.quarkus.it.compose.devservices.kafka;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * A broker that is only healthy once Quarkus has copied the exposed ports file must not block the services that
 * depend on it being healthy.
 */
@QuarkusTest
@TestProfile(HealthyDependencyTestProfile.class)
public class KafkaHealthyDependencyTest {

    @Test
    public void dependentServiceIsStartedOnceTheBrokerIsHealthy() {
        when().put("/kafka/topics/healthy/2").then().statusCode(204);
        when().get("/kafka/partitions/healthy").then().body(Matchers.is("2"));

        boolean consumerRunning = DockerClientFactory.lazyClient().listContainersCmd()
                .withLabelFilter(Map.of("com.docker.compose.service", "consumer"))
                .exec().stream()
                .filter(container -> container.getLabels().getOrDefault("com.docker.compose.project", "")
                        .startsWith("healthy-dependency"))
                .anyMatch(container -> "running".equals(container.getState()));
        assertTrue(consumerRunning, "the service depending on the healthy broker should be running");
    }

}
