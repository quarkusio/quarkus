package io.quarkus.resteasy.jackson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class TimeCustomizer implements ObjectMapperCustomizer {

    public static final String HELLO = "hello";

    @Override
    public void customize(ObjectMapper objectMapper) {
        JavaTimeModule customDateModule = new JavaTimeModule();
        customDateModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(
                new DateTimeFormatterBuilder().appendInstant(0).toFormatter().withZone(ZoneId.of("Z"))));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(customDateModule);
    }
}
