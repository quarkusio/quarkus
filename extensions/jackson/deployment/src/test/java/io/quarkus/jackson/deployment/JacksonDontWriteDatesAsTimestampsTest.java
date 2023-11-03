package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonDontWriteDatesAsTimestampsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testDateWrittenAsString() throws JsonMappingException, JsonProcessingException {
        Pojo pojo = new Pojo();
        pojo.zonedDateTime = ZonedDateTime.of(1988, 11, 17, 0, 0, 0, 0, ZoneId.of("GMT"));

        assertThat(objectMapper.writeValueAsString(pojo)).contains("1988-11-17T00:00:00Z");
    }

    public static class Pojo {

        public ZonedDateTime zonedDateTime;
    }
}
