package io.quarkus.it.kafka.streams;

import static org.apache.kafka.streams.KafkaStreams.State;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.kafka.streams.KafkaStreams;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;
import io.quarkus.runtime.Application;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTestResource(KafkaSSLTestResource.class)
@TestProfile(KafkaStreamsStartupFailureTest.NonExistingTopicProfile.class)
@QuarkusTest
public class KafkaStreamsStartupFailureTest {

    @Inject
    KafkaStreams kafkaStreams;

    @Inject
    KafkaStreamsTopologyManager kafkaStreamsTopologyManager;

    @Test
    @Timeout(5)
    public void testShutdownBeforeKStreamsStarted() throws Exception {
        assertEquals(State.CREATED, kafkaStreams.state());
        RestAssured.get("/q/health/ready").then()
                .statusCode(HttpStatus.SC_SERVICE_UNAVAILABLE)
                .body("checks[0].name", CoreMatchers.is("Kafka Streams topics health check"))
                .body("checks[0].status", CoreMatchers.is("DOWN"))
                .body("checks[0].data.missing_topics", CoreMatchers.is("nonexisting-topic"));
        assertTrue(Application.currentApplication().isStarted());

        Application.currentApplication().stop();

        assertEquals(State.NOT_RUNNING, kafkaStreams.state());
    }

    public static class NonExistingTopicProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> conf = new HashMap<>();
            conf.put("quarkus.kafka-streams.topics", "nonexisting-topic");
            return conf;
        }
    }
}
