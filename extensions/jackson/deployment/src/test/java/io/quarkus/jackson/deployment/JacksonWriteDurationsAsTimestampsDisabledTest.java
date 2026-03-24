package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusExtensionTest;

public class JacksonWriteDurationsAsTimestampsDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-write-durations-as-timestamps-disabled.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testDurationWrittenAsIso8601() throws JsonProcessingException {
        Pojo pojo = new Pojo();
        pojo.duration = Duration.ofMillis(65542516);

        assertThat(objectMapper.writeValueAsString(pojo)).isEqualTo("{\"duration\":\"PT18H12M22.516S\"}");
    }

    public static class Pojo {

        public Duration duration;
    }
}
