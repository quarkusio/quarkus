package io.quarkus.it.kafka.streams;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTestResource(KafkaSSLTestResource.class)
@TestProfile(KafkaStreamsTopicsPatternTest.TopicsPatternProfile.class)
@QuarkusTest
public class KafkaStreamsTopicsPatternTest extends KafkaStreamsTest {

    @Override
    Matcher<String> topicsMatcher() {
        return CoreMatchers.containsString("streams-test-.*");
    }

    public static class TopicsPatternProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> conf = new HashMap<>();
            conf.put("quarkus.kafka-streams.topic-patterns", "streams-test-.*");
            return conf;
        }
    }
}
