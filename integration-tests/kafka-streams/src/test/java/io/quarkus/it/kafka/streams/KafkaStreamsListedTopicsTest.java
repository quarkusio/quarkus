package io.quarkus.it.kafka.streams;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTestResource(KafkaSSLTestResource.class)
@TestProfile(KafkaStreamsListedTopicsTest.ListedTopicsProfile.class)
@QuarkusTest
public class KafkaStreamsListedTopicsTest extends KafkaStreamsTest {

    public static class ListedTopicsProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> conf = new HashMap<>();
            conf.put("quarkus.kafka-streams.topics", "streams-test-categories,streams-test-customers");
            return conf;
        }
    }
}
