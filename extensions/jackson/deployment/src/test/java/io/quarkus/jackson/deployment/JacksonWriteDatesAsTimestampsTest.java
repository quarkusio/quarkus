package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonWriteDatesAsTimestampsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-write-dates-as-timestamps.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testDateWrittenAsNumericValue() {
        Pojo pojo = new Pojo();
        pojo.zonedDateTime = ZonedDateTime.of(1988, 11, 17, 0, 0, 0, 0, ZoneId.of("GMT"));

        assertThat(objectMapper.writeValueAsString(pojo)).contains("595728000");
    }

    public static class Pojo {

        public ZonedDateTime zonedDateTime;
    }
}
