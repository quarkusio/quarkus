package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonWriteDurationsAsTimestampsEnabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-write-durations-as-timestamps-enabled.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testDurationWrittenAsTimestamp() {
        Pojo pojo = new Pojo();
        pojo.duration = Duration.ofMillis(65542516);

        assertThat(objectMapper.writeValueAsString(pojo)).isEqualTo("{\"duration\":65542.516000000}");
    }

    public static class Pojo {

        public Duration duration;
    }
}
