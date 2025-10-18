package io.quarkus.jackson.deployment;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonTimeZonePropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-timezone-properties.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testTimezone() throws JacksonException {
        Assertions.assertThat(objectMapper.writeValueAsString(new Pojo(Date.from(
                ZonedDateTime.of(LocalDateTime.of(2021, Month.MARCH, 3, 11, 5), ZoneId.of("GMT")).toInstant()))))
                .contains("+07");
    }

    public static class Pojo {

        private final Date date;

        public Pojo(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }
}
