package io.quarkus.jackson.deployment;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonTimeZonePropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-timezone-properties.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testTimezone() throws JsonProcessingException {
        Assertions.assertThat(objectMapper.writeValueAsString(new Pojo(new Date(2021, Calendar.MARCH, 3, 11, 5))))
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
