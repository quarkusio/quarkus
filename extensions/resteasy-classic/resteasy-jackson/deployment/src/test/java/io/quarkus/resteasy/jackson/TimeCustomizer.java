package io.quarkus.resteasy.jackson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import jakarta.inject.Singleton;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class TimeCustomizer implements ObjectMapperCustomizer {

    public static final String HELLO = "hello";

    @Override
    public void customize(ObjectMapper objectMapper) {
        // ALEX OUCH
        JavaTimeModule customDateModule = new JavaTimeModule();
        customDateModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(
                new DateTimeFormatterBuilder().appendInstant(0).toFormatter().withZone(ZoneId.of("Z"))));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(customDateModule);
    }
}
