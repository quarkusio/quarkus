package io.quarkus.it.kafka.streams;

import java.lang.reflect.Field;
import java.util.Map;

import javax.inject.Inject;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(KafkaSSLTestResource.class)
@QuarkusTest
public class KafkaStreamsPropertiesTest {

    @Inject
    KafkaStreams streams;

    @Test
    public void testProperties() throws Exception {
        // reflection hack ... no other way to get raw props ...
        Field configField = KafkaStreams.class.getDeclaredField("config");
        configField.setAccessible(true);
        StreamsConfig config = (StreamsConfig) configField.get(streams);

        Map<String, Object> originals = config.originals();

        Assertions.assertEquals("20", originals.get(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS));
        Assertions.assertEquals("http://localhost:8080", originals.get("apicurio.registry.url"));
        Assertions.assertEquals("dummy", originals.get("some-property"));
    }
}
