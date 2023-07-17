package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonWriteDurationsAsTimestampsEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-write-durations-as-timestamps-enabled.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testDurationWrittenAsTimestamp() throws JsonProcessingException {
        Pojo pojo = new Pojo();
        pojo.duration = Duration.ofMillis(65542516);

        assertThat(objectMapper.writeValueAsString(pojo)).isEqualTo("{\"duration\":65542.516000000}");
    }

    public static class Pojo {

        public Duration duration;
    }
}
